package com.fixlink.adapter.in.web.dto.response;

import com.fixlink.domain.model.RequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepairRequestResponse {
    private Long id;
    private String requestCode;
    private Long customerId;
    private Long technicianId;
    private Long categoryId;
    private String categoryName;
    private Long areaId;
    private String areaName;
    private RequestStatus status;
    private String title;
    private String description;
    private String addressLine;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private LocalDateTime preferredTime;
    private BigDecimal agreedPrice;
    private BigDecimal depositAmount;
    private BigDecimal budgetRef;
    private LocalDateTime biddingDeadline;
    private String cancelReason;
    private Long selectedQuotationId;
    private List<String> mediaUrls;
    private int quotationCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
