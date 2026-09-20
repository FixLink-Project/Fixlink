package com.fixlink.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Thực thể gói dịch vụ sửa chữa chi tiết (Jira RC-8).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Service {
    private Long id;
    private Long categoryId;
    private String code;
    private String name;
    private String description;
    private BigDecimal basePrice;
    private Integer estimatedDurationMinutes;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private Long createdBy;
    private LocalDateTime updatedAt;
    private Long updatedBy;
    private LocalDateTime deletedAt;
    private Long deletedBy;
}
