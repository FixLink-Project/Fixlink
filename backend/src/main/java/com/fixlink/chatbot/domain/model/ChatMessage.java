package com.fixlink.chatbot.domain.model;

import java.time.Instant;
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
public class ChatMessage {
    private String id;
    private String role; // "user" | "model" | "system"
    private String content;
    private List<SourceReference> sources;
    private List<ChatAction> actions;
    private List<String> suggestions;
    private boolean needsHuman;
    private String ticketId;
    private String modelUsed;
    private Instant timestamp;
}
