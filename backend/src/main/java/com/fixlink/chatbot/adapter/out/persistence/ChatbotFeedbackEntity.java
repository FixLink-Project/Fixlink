package com.fixlink.chatbot.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "chatbot_feedback")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotFeedbackEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, length = 100)
    private String sessionId;

    @Column(name = "message_id", length = 100)
    private String messageId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "rating", nullable = false, length = 10)
    private String rating;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
