package com.fixlink.application.service;

import com.fixlink.adapter.in.web.dto.response.NotificationResponse;
import com.fixlink.adapter.out.persistence.entity.NotificationJpaEntity;
import com.fixlink.adapter.out.persistence.repository.SpringDataNotificationRepository;
import com.fixlink.application.port.in.NotificationUseCase;
import com.fixlink.domain.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService implements NotificationUseCase {

    private final SpringDataNotificationRepository notificationRepo;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendNotification(Long userId, String title, String message, String type, String referenceId, String referenceUrl) {
        try {
            if (userId == null) {
                log.warn("[NotificationService] Cannot send notification: userId is null");
                return;
            }

            NotificationJpaEntity entity = NotificationJpaEntity.builder()
                    .userId(userId)
                    .title(title)
                    .message(message)
                    .type(type)
                    .referenceId(referenceId)
                    .referenceUrl(referenceUrl)
                    .isRead(false)
                    .build();

            notificationRepo.save(entity);
            log.info("[NotificationService] Sent notification to user {}: [{}] {}", userId, type, title);
        } catch (Exception ex) {
            log.error("[NotificationService] Failed to send notification to user {}: {}", userId, ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications(Long userId, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        return notificationRepo.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(
                userId,
                PageRequest.of(0, safeLimit)
        ).map(NotificationResponse::fromEntity).getContent();
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepo.countByUserIdAndIsReadFalseAndDeletedAtIsNull(userId);
    }

    @Override
    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        NotificationJpaEntity entity = notificationRepo.findByIdAndDeletedAtIsNull(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông báo"));

        if (!entity.getUserId().equals(userId)) {
            log.warn("[NotificationService] User {} attempted to mark notification {} belonging to user {}", userId, notificationId, entity.getUserId());
            return;
        }

        entity.setIsRead(true);
        notificationRepo.save(entity);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepo.markAllAsReadByUserId(userId);
    }
}
