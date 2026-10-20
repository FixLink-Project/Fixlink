package com.fixlink.chatbot.adapter.out.rag;

import com.fixlink.chatbot.domain.model.RagChunk;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class DocumentContextRegistry {

    // Key: documentName -> (chunkIndex -> RagChunk)
    private final Map<String, Map<Integer, RagChunk>> registry = new ConcurrentHashMap<>();

    public void registerChunk(RagChunk chunk) {
        if (chunk == null || chunk.getDocumentName() == null) {
            return;
        }
        registry.computeIfAbsent(chunk.getDocumentName(), k -> new ConcurrentHashMap<>())
                .put(chunk.getChunkIndex(), chunk);
    }

    public Optional<RagChunk> getChunk(String documentName, int chunkIndex) {
        Map<Integer, RagChunk> docChunks = registry.get(documentName);
        if (docChunks == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(docChunks.get(chunkIndex));
    }

    public void clear() {
        registry.clear();
    }
}
