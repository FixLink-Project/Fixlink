package com.fixlink.chatbot.domain.model;

import java.time.Instant;
import java.util.ArrayList;
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
public class ChatSession {
    private String sessionId;
    private Long userId;
    private String role; // "GUEST" | "CUSTOMER" | "TECHNICIAN" | "ADMIN"
    @Builder.Default
    private List<ChatMessage> messages = new ArrayList<>();
    private Instant createdAt;
    private Instant updatedAt;
}
