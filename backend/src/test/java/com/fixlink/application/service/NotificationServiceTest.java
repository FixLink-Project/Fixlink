package com.fixlink.application.service;

import com.fixlink.adapter.in.web.dto.response.NotificationResponse;
import com.fixlink.adapter.out.persistence.entity.NotificationJpaEntity;
import com.fixlink.adapter.out.persistence.repository.SpringDataNotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private SpringDataNotificationRepository notificationRepo;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    @DisplayName("Gửi thông báo thành công - lưu đúng entity vào DB")
    void sendNotification_success() {
        notificationService.sendNotification(
                100L,
                "Có báo giá mới từ thợ",
                "Thợ Nguyễn Văn A vừa gửi báo giá 500.000đ",
                "QUOTATION_RECEIVED",
                "10",
                "/yeu-cau/10"
        );

        ArgumentCaptor<NotificationJpaEntity> captor = ArgumentCaptor.forClass(NotificationJpaEntity.class);
        verify(notificationRepo).save(captor.capture());

        NotificationJpaEntity saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(100L);
        assertThat(saved.getTitle()).isEqualTo("Có báo giá mới từ thợ");
        assertThat(saved.getMessage()).contains("500.000đ");
        assertThat(saved.getType()).isEqualTo("QUOTATION_RECEIVED");
        assertThat(saved.getReferenceId()).isEqualTo("10");
        assertThat(saved.getReferenceUrl()).isEqualTo("/yeu-cau/10");
        assertThat(saved.getIsRead()).isFalse();
    }

    @Test
    @DisplayName("Gửi thông báo khi userId null không ném lỗi")
    void sendNotification_nullUserId_safe() {
        notificationService.sendNotification(null, "Title", "Message", "TYPE", "1", "/url");
        verify(notificationRepo, never()).save(any());
    }

    @Test
    @DisplayName("Lấy danh sách thông báo của người dùng")
    void getMyNotifications_success() {
        NotificationJpaEntity entity = NotificationJpaEntity.builder()
                .id(1L)
                .userId(100L)
                .title("Thông báo test")
                .message("Nội dung")
                .type("TEST")
                .isRead(false)
                .build();
        entity.setCreatedAt(LocalDateTime.now());

        when(notificationRepo.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(eq(100L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity)));

        List<NotificationResponse> list = notificationService.getMyNotifications(100L, 10);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getTitle()).isEqualTo("Thông báo test");
        assertThat(list.get(0).getIsRead()).isFalse();
    }

    @Test
    @DisplayName("Đếm số thông báo chưa đọc")
    void getUnreadCount_success() {
        when(notificationRepo.countByUserIdAndIsReadFalseAndDeletedAtIsNull(100L)).thenReturn(3L);

        long count = notificationService.getUnreadCount(100L);
        assertThat(count).isEqualTo(3L);
    }

    @Test
    @DisplayName("Đánh dấu một thông báo đã đọc")
    void markAsRead_success() {
        NotificationJpaEntity entity = NotificationJpaEntity.builder()
                .id(1L)
                .userId(100L)
                .isRead(false)
                .build();

        when(notificationRepo.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(entity));

        notificationService.markAsRead(100L, 1L);

        assertThat(entity.getIsRead()).isTrue();
        verify(notificationRepo).save(entity);
    }

    @Test
    @DisplayName("Đánh dấu tất cả thông báo đã đọc")
    void markAllAsRead_success() {
        when(notificationRepo.markAllAsReadByUserId(100L)).thenReturn(5);

        notificationService.markAllAsRead(100L);

        verify(notificationRepo).markAllAsReadByUserId(100L);
    }
}
