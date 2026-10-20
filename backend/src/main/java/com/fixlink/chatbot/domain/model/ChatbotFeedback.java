package com.fixlink.chatbot.domain.model;

import java.time.Instant;
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
public class ChatbotFeedback {
    private Long id;
    private String sessionId;
    private String messageId;
    private Long userId;
    private String rating; // "UP" | "DOWN"
    private String comment;
    private Instant createdAt;
}
