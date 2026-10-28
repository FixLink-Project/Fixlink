package com.fixlink.application.port.in;

import com.fixlink.adapter.in.web.dto.response.NotificationResponse;

import java.util.List;

public interface NotificationUseCase {

    void sendNotification(Long userId, String title, String message, String type, String referenceId, String referenceUrl);

    List<NotificationResponse> getMyNotifications(Long userId, int limit);

    long getUnreadCount(Long userId);

    void markAsRead(Long userId, Long notificationId);

    void markAllAsRead(Long userId);
}
