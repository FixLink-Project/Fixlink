package com.fixlink.chatbot.adapter.out.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatbotFeedbackJpaRepository extends JpaRepository<ChatbotFeedbackEntity, Long> {

    List<ChatbotFeedbackEntity> findBySessionId(String sessionId);

    List<ChatbotFeedbackEntity> findByUserId(Long userId);
}
