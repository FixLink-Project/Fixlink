package com.fixlink.chatbot.adapter.out.rag;

import com.fixlink.chatbot.domain.model.RagChunk;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

@Component
public class MarkdownDocumentLoader {

    private static final Logger LOG = LoggerFactory.getLogger(MarkdownDocumentLoader.class);

    private final DocumentContextRegistry contextRegistry;

    public MarkdownDocumentLoader(DocumentContextRegistry contextRegistry) {
        this.contextRegistry = contextRegistry;
    }

    public List<RagChunk> loadAllDocuments() {
        List<RagChunk> allChunks = new ArrayList<>();
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:chatbot/knowledge/*.md");
            LOG.info("Found {} markdown knowledge files in classpath:chatbot/knowledge/", resources.length);

            contextRegistry.clear();

            for (Resource resource : resources) {
                String filename = resource.getFilename();
                try (InputStream is = resource.getInputStream();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    List<RagChunk> chunks = parseAndChunkMarkdown(filename, sb.toString());
                    for (RagChunk chunk : chunks) {
                        contextRegistry.registerChunk(chunk);
                    }
                    allChunks.addAll(chunks);
                } catch (Exception ex) {
                    LOG.error("Failed to load knowledge file: {}", filename, ex);
                }
            }
        } catch (Exception e) {
            LOG.error("Error scanning chatbot knowledge files", e);
        }
        return allChunks;
    }

    public List<RagChunk> parseAndChunkMarkdown(String filename, String rawContent) {
        List<RagChunk> chunks = new ArrayList<>();
        String content = rawContent;
        String title = filename;
        List<String> audience = new ArrayList<>(List.of("guest", "customer", "technician", "admin"));

        // Parse front-matter
        if (content.startsWith("---")) {
            int endFm = content.indexOf("---", 3);
            if (endFm != -1) {
                String frontMatter = content.substring(3, endFm).trim();
                content = content.substring(endFm + 3).trim();
                for (String fLine : frontMatter.split("\n")) {
                    fLine = fLine.trim();
                    if (fLine.startsWith("title:")) {
                        title = fLine.replace("title:", "").replace("\"", "").trim();
                    } else if (fLine.startsWith("audience:")) {
                        String audStr = fLine.replace("audience:", "").replace("[", "").replace("]", "").trim();
                        audience = Arrays.stream(audStr.split(","))
                                .map(String::trim)
                                .map(String::toLowerCase)
                                .toList();
                    }
                }
            }
        }

        // Chunking by Markdown Headings (# or ## or ###)
        String[] lines = content.split("\n");
        String currentSection = title;
        StringBuilder currentText = new StringBuilder();
        int chunkIndex = 0;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("# ") || trimmed.startsWith("## ") || trimmed.startsWith("### ")) {
                if (currentText.length() > 0) {
                    String text = currentText.toString().trim();
                    if (!text.isEmpty()) {
                        String chunkId = filename + "#" + chunkIndex;
                        chunks.add(RagChunk.builder()
                                .id(chunkId)
                                .documentName(filename)
                                .section(currentSection)
                                .content(text)
                                .audience(audience)
                                .chunkIndex(chunkIndex++)
                                .build());
                    }
                    currentText.setLength(0);
                }
                currentSection = trimmed.replaceFirst("^#+\\s*", "");
                currentText.append(line).append("\n");
            } else {
                currentText.append(line).append("\n");
            }
        }

        if (currentText.length() > 0) {
            String text = currentText.toString().trim();
            if (!text.isEmpty()) {
                String chunkId = filename + "#" + chunkIndex;
                chunks.add(RagChunk.builder()
                        .id(chunkId)
                        .documentName(filename)
                        .section(currentSection)
                        .content(text)
                        .audience(audience)
                        .chunkIndex(chunkIndex++)
                        .build());
            }
        }

        return chunks;
    }
}
