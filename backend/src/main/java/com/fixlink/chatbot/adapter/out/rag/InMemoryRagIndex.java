package com.fixlink.chatbot.adapter.out.rag;

import com.fixlink.chatbot.adapter.out.gemini.DeterministicFastEmbeddingModel;
import com.fixlink.chatbot.domain.model.RagChunk;
import com.fixlink.chatbot.domain.model.SourceReference;
import com.fixlink.chatbot.infrastructure.config.ChatbotProperties;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class InMemoryRagIndex {

    private static final Logger LOG = LoggerFactory.getLogger(InMemoryRagIndex.class);

    private final MarkdownDocumentLoader loader;
    private final DocumentContextRegistry contextRegistry;
    private final ChatbotProperties properties;

    public record IndexSnapshot(
            List<RagChunk> chunks,
            Map<String, Integer> docFrequencies,
            double avgDocLength
    ) {
        public IndexSnapshot {
            chunks = List.copyOf(chunks);
            docFrequencies = Map.copyOf(docFrequencies);
        }
    }

    private final AtomicReference<IndexSnapshot> snapshotRef = new AtomicReference<>(
            new IndexSnapshot(List.of(), Map.of(), 50.0)
    );

    /**
     * Toàn bộ kho tri thức ghép thành một văn bản. Kho tri thức FixLink nhỏ (vài chục KB) nên
     * được nạp nguyên vào system prompt: model luôn thấy đủ quy định, không phụ thuộc việc
     * truy hồi RAG có trúng đoạn cần thiết hay không.
     */
    private final AtomicReference<String> fullKnowledgeRef = new AtomicReference<>("");

    public InMemoryRagIndex(
            MarkdownDocumentLoader loader,
            DocumentContextRegistry contextRegistry,
            ChatbotProperties properties
    ) {
        this.loader = loader;
        this.contextRegistry = contextRegistry;
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        reindex();
    }

    public synchronized void reindex() {
        LOG.info("Starting knowledge base indexing...");
        List<RagChunk> chunks = loader.loadAllDocuments();
        List<RagChunk> newChunks = new ArrayList<>();
        Map<String, Integer> newDocFrequencies = new HashMap<>();

        long totalTokens = 0;
        for (RagChunk chunk : chunks) {
            // Compute deterministic embedding
            chunk.setEmbedding(DeterministicFastEmbeddingModel.embed(chunk.getSection() + "\n" + chunk.getContent()));

            // Document frequency for BM25
            Set<String> uniqueTokens = Bm25Scorer.tokenize(chunk.getContent());
            for (String token : uniqueTokens) {
                newDocFrequencies.merge(token, 1, Integer::sum);
            }
            totalTokens += Bm25Scorer.tokenizeList(chunk.getContent()).size();
            newChunks.add(chunk);
        }

        double newAvgDocLength = newChunks.isEmpty() ? 50.0 : (double) totalTokens / newChunks.size();
        snapshotRef.set(new IndexSnapshot(newChunks, newDocFrequencies, newAvgDocLength));
        fullKnowledgeRef.set(buildFullKnowledgeText(newChunks));

        LOG.info("Indexed {} knowledge chunks successfully. Avg doc length: {}", newChunks.size(), newAvgDocLength);
    }

    public record RetrievalResult(
            String mergedContent,
            List<SourceReference> sources,
            double maxScore
    ) { }

    public RetrievalResult retrieve(String query, String userRole) {
        IndexSnapshot snapshot = snapshotRef.get();
        List<RagChunk> indexedChunks = snapshot.chunks();
        Map<String, Integer> docFrequencies = snapshot.docFrequencies();
        double avgDocLength = snapshot.avgDocLength();

        if (query == null || query.isBlank() || indexedChunks.isEmpty()) {
            return new RetrievalResult("", List.of(), 0.0);
        }

        String role = userRole != null ? userRole.toLowerCase().trim() : "guest";

        // 1. Filter by audience
        List<RagChunk> eligible = indexedChunks.stream()
                .filter(c -> c.getAudience() == null || c.getAudience().contains(role) || c.getAudience().contains("guest"))
                .toList();

        if (eligible.isEmpty()) {
            return new RetrievalResult("", List.of(), 0.0);
        }

        // 2. BM25 scoring & ranking
        int totalDocs = eligible.size();
        Map<RagChunk, Double> bm25Scores = new HashMap<>();
        for (RagChunk chunk : eligible) {
            double score = Bm25Scorer.computeScore(
                    query,
                    chunk.getSection() + "\n" + chunk.getContent(),
                    avgDocLength,
                    totalDocs,
                    docFrequencies
            );
            bm25Scores.put(chunk, score);
        }

        List<RagChunk> bm25Ranking = new ArrayList<>(eligible);
        bm25Ranking.sort((a, b) -> Double.compare(bm25Scores.getOrDefault(b, 0.0), bm25Scores.getOrDefault(a, 0.0)));

        // 3. Vector similarity scoring & ranking
        double[] queryEmbedding = DeterministicFastEmbeddingModel.embed(query);
        Map<RagChunk, Double> vectorScores = new HashMap<>();
        for (RagChunk chunk : eligible) {
            double sim = DeterministicFastEmbeddingModel.cosineSimilarity(queryEmbedding, chunk.getEmbedding());
            vectorScores.put(chunk, sim);
        }

        List<RagChunk> vectorRanking = new ArrayList<>(eligible);
        vectorRanking.sort((a, b) -> Double.compare(vectorScores.getOrDefault(b, 0.0), vectorScores.getOrDefault(a, 0.0)));

        // 4. Reciprocal Rank Fusion (RRF)
        // BM25 (khớp từ khóa thật) được tin hơn embedding băm cục bộ nên có trọng số gấp đôi.
        Map<RagChunk, Double> rrfScores = ReciprocalRankFusion.fuseWeighted(vectorRanking, 0.5, bm25Ranking, 1.0);

        // Sort by RRF score
        List<RagChunk> rrfRanked = new ArrayList<>(eligible);
        rrfRanked.sort((a, b) -> Double.compare(rrfScores.getOrDefault(b, 0.0), rrfScores.getOrDefault(a, 0.0)));

        int topK = properties.getRag().getTopK();
        double minScore = properties.getRag().getMinScore();

        List<RagChunk> topResults = rrfRanked.stream()
                .filter(c -> {
                    double vScore = vectorScores.getOrDefault(c, 0.0);
                    double bScore = bm25Scores.getOrDefault(c, 0.0);
                    // Match either semantic or keyword
                    return vScore >= minScore || bScore > 0.5;
                })
                .limit(topK)
                .toList();

        if (topResults.isEmpty()) {
            LOG.info("No knowledge chunks met minimum score threshold ({}) for query '{}'", minScore, query);
            return new RetrievalResult("", List.of(), 0.0);
        }

        // 5. Window Retrieval Expansion (i±1)
        int window = properties.getRag().getWindow();
        Set<String> includedChunkIds = new HashSet<>();
        List<RagChunk> expandedList = new ArrayList<>();
        List<SourceReference> sources = new ArrayList<>();
        double highestScore = 0.0;

        for (RagChunk hit : topResults) {
            double s = vectorScores.getOrDefault(hit, 0.0);
            if (s > highestScore) {
                highestScore = s;
            }

            sources.add(new SourceReference(hit.getDocumentName(), hit.getSection()));

            if (window > 0) {
                // i - 1
                Optional<RagChunk> prev = contextRegistry.getChunk(hit.getDocumentName(), hit.getChunkIndex() - 1);
                prev.ifPresent(p -> {
                    if (includedChunkIds.add(p.getId())) {
                        expandedList.add(p);
                    }
                });

                // i
                if (includedChunkIds.add(hit.getId())) {
                    expandedList.add(hit);
                }

                // i + 1
                Optional<RagChunk> next = contextRegistry.getChunk(hit.getDocumentName(), hit.getChunkIndex() + 1);
                next.ifPresent(n -> {
                    if (includedChunkIds.add(n.getId())) {
                        expandedList.add(n);
                    }
                });
            } else {
                if (includedChunkIds.add(hit.getId())) {
                    expandedList.add(hit);
                }
            }
        }

        // 6. Merge retrieved chunks into context string
        StringBuilder merged = new StringBuilder();
        for (RagChunk chunk : expandedList) {
            merged.append("### ").append(chunk.getSection()).append(" (Nguồn: ").append(chunk.getDocumentName()).append(")\n");
            merged.append(chunk.getContent()).append("\n\n");
        }

        // Deduplicate sources
        List<SourceReference> distinctSources = sources.stream()
                .distinct()
                .toList();

        LOG.info("RAG retrieved {} chunks (expanded from {} hits) for query '{}'", expandedList.size(), topResults.size(), query);
        return new RetrievalResult(merged.toString().trim(), distinctSources, highestScore);
    }

    private String buildFullKnowledgeText(List<RagChunk> chunks) {
        Map<String, List<RagChunk>> byDocument = new TreeMap<>();
        for (RagChunk chunk : chunks) {
            byDocument.computeIfAbsent(chunk.getDocumentName(), k -> new ArrayList<>()).add(chunk);
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, List<RagChunk>> entry : byDocument.entrySet()) {
            List<RagChunk> docChunks = new ArrayList<>(entry.getValue());
            docChunks.sort(Comparator.comparingInt(RagChunk::getChunkIndex));
            sb.append("<tai-lieu ten=\"").append(entry.getKey()).append("\">\n");
            for (RagChunk chunk : docChunks) {
                sb.append(chunk.getContent()).append("\n\n");
            }
            sb.append("</tai-lieu>\n\n");
        }
        return sb.toString().trim();
    }

    /** Toàn bộ nội dung kho tri thức (đã bỏ front-matter), dùng để nạp vào system prompt. */
    public String getFullKnowledgeText() {
        return fullKnowledgeRef.get();
    }

    public int getIndexedCount() {
        return snapshotRef.get().chunks().size();
    }
}
