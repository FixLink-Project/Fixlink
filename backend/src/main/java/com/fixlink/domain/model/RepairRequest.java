package com.fixlink.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Thực thể lõi yêu cầu sửa chữa (Jira RC-8).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepairRequest {
    private Long id;
    private String requestCode;
    private Long customerId;
    private Long technicianId;
    private Long categoryId;
    private Long serviceId;
    private RequestStatus status;
    private String title;
    private String description;
    private String address;
    private LocalDateTime requestedTime;
    private BigDecimal agreedPrice;
    private BigDecimal depositAmount;
    private Long version;
    private LocalDateTime createdAt;
    private Long createdBy;
    private LocalDateTime updatedAt;
    private Long updatedBy;
    private LocalDateTime deletedAt;
    private Long deletedBy;
}
