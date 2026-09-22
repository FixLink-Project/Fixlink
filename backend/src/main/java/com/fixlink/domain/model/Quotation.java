package com.fixlink.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Quotation {
    private Long id;
    private Long requestId;
    private Long technicianId;
    private String solution;
    private BigDecimal priceLaborVnd;
    private BigDecimal priceMaterialsVnd;
    private LocalDateTime inspectionTime;
    private LocalDateTime estimatedFinish;
    private String note;
    private QuotationStatus status;
    private LocalDateTime createdAt;
    private Long createdBy;
    private LocalDateTime updatedAt;
    private Long updatedBy;

    public BigDecimal totalPrice() {
        BigDecimal labor = priceLaborVnd != null ? priceLaborVnd : BigDecimal.ZERO;
        BigDecimal materials = priceMaterialsVnd != null ? priceMaterialsVnd : BigDecimal.ZERO;
        return labor.add(materials);
    }
}
