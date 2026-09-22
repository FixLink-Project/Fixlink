package com.fixlink.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
    private Long areaId;
    private RequestStatus status;
    private String title;
    private String description;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private LocalDateTime requestedTime;
    private BigDecimal agreedPrice;
    private BigDecimal depositAmount;
    private BigDecimal budgetRef;
    private LocalDateTime biddingDeadline;
    private String cancelReason;
    private Long selectedQuotationId;
    private Long version;
    private LocalDateTime createdAt;
    private Long createdBy;
    private LocalDateTime updatedAt;
    private Long updatedBy;
    private LocalDateTime deletedAt;
    private Long deletedBy;

    private List<String> mediaUrls;

    public boolean canEdit() {
        return status == RequestStatus.DRAFT || status == RequestStatus.BIDDING_OPEN;
    }

    public boolean canCancel() {
        return status != RequestStatus.IN_PROGRESS
                && status != RequestStatus.AWAITING_ACCEPTANCE
                && status != RequestStatus.COMPLETED
                && status != RequestStatus.CANCELLED;
    }

    public boolean isBiddingExpired() {
        return biddingDeadline != null && LocalDateTime.now().isAfter(biddingDeadline);
    }
}
