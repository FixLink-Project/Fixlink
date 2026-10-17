package com.fixlink.chatbot.domain.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagChunk {
    private String id;
    private String documentName;
    private String section;
    private String content;
    private List<String> audience; // e.g. ["guest", "customer", "technician", "admin"]
    private int chunkIndex;
    private double[] embedding;
}
