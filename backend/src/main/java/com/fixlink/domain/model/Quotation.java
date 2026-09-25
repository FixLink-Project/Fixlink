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

    public Long getRepairRequestId() {
        return requestId;
    }

    public void setRepairRequestId(Long repairRequestId) {
        this.requestId = repairRequestId;
    }

    public BigDecimal getPriceLabor() {
        return priceLaborVnd;
    }

    public void setPriceLabor(BigDecimal priceLabor) {
        this.priceLaborVnd = priceLabor;
    }

    public BigDecimal getPriceMaterials() {
        return priceMaterialsVnd;
    }

    public void setPriceMaterials(BigDecimal priceMaterials) {
        this.priceMaterialsVnd = priceMaterials;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice();
    }

    public BigDecimal calculateTotalPrice() {
        return totalPrice();
    }

    public void validate() {
        if (solution == null || solution.trim().isEmpty()) {
            throw new com.fixlink.domain.exception.DomainException("INVALID_QUOTATION_DATA", "Phương án sửa chữa không được để trống", 400);
        }
        if (priceLaborVnd == null || priceLaborVnd.compareTo(BigDecimal.ZERO) < 0) {
            throw new com.fixlink.domain.exception.DomainException("INVALID_PRICE", "Chi phí nhân công không được âm", 400);
        }
        if (priceMaterialsVnd == null || priceMaterialsVnd.compareTo(BigDecimal.ZERO) < 0) {
            throw new com.fixlink.domain.exception.DomainException("INVALID_PRICE", "Chi phí vật tư không được âm", 400);
        }
        if (totalPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new com.fixlink.domain.exception.DomainException("INVALID_PRICE", "Tổng giá trị báo giá phải lớn hơn 0", 400);
        }
    }

    public void withdraw() {
        if (this.status != QuotationStatus.PENDING) {
            throw new com.fixlink.domain.exception.DomainException("INVALID_STATE", "Chỉ có thể rút lại báo giá khi đang chờ phản hồi (PENDING)", 400);
        }
        this.status = QuotationStatus.WITHDRAWN;
        this.updatedAt = LocalDateTime.now();
    }

    public void accept() {
        this.status = QuotationStatus.ACCEPTED;
        this.updatedAt = LocalDateTime.now();
    }

    public void reject() {
        this.status = QuotationStatus.REJECTED;
        this.updatedAt = LocalDateTime.now();
    }

    public static class QuotationBuilder {
        public QuotationBuilder repairRequestId(Long repairRequestId) {
            this.requestId = repairRequestId;
            return this;
        }

        public QuotationBuilder priceLabor(BigDecimal priceLabor) {
            this.priceLaborVnd = priceLabor;
            return this;
        }

        public QuotationBuilder priceMaterials(BigDecimal priceMaterials) {
            this.priceMaterialsVnd = priceMaterials;
            return this;
        }
    }
}
