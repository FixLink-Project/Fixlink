package com.fixlink.adapter.in.web.dto.response;

import com.fixlink.domain.model.RequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcceptQuotationResponse {
    private Long requestId;
    private Long selectedQuotationId;
    private String technicianName;
    private BigDecimal agreedPrice;
    private BigDecimal depositAmount;
    private RequestStatus status;
}
