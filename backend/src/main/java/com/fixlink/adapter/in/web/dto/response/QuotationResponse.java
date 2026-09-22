package com.fixlink.adapter.in.web.dto.response;

import com.fixlink.domain.model.QuotationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotationResponse {
    private Long id;
    private Long requestId;
    private Long technicianId;
    private String technicianName;
    private BigDecimal avgRating;
    private Integer completedJobs;
    private Integer yearsExperience;
    private String solution;
    private BigDecimal priceLaborVnd;
    private BigDecimal priceMaterialsVnd;
    private BigDecimal totalPrice;
    private LocalDateTime inspectionTime;
    private LocalDateTime estimatedFinish;
    private String note;
    private QuotationStatus status;
    private LocalDateTime createdAt;
}
