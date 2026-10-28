package com.fixlink.adapter.in.web.dto.response;

import com.fixlink.adapter.out.persistence.entity.NotificationJpaEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {
    private Long id;
    private Long userId;
    private String title;
    private String message;
    private String type;
    private String referenceId;
    private String referenceUrl;
    private Boolean isRead;
    private LocalDateTime createdAt;

    public static NotificationResponse fromEntity(NotificationJpaEntity entity) {
        if (entity == null) return null;
        return NotificationResponse.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .title(entity.getTitle())
                .message(entity.getMessage())
                .type(entity.getType())
                .referenceId(entity.getReferenceId())
                .referenceUrl(entity.getReferenceUrl())
                .isRead(entity.getIsRead())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
