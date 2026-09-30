package com.fixlink.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Bản ghi thợ nhận việc của một yêu cầu sửa chữa.
 *
 * <p><b>Lưu ý khi dùng lại lớp này.</b> Sau khi pivot sang mô hình "ai nhận
 * trước được trước", bảng {@code quotations} không còn là "nhiều báo giá cạnh
 * tranh" nữa: mỗi yêu cầu có <b>tối đa một</b> bản ghi, tạo ra tại thời điểm thợ
 * bấm "Nhận việc", với {@code status = ACCEPTED} và giá lấy nguyên từ
 * {@code budgetRef} của yêu cầu. Hai field {@code priceLaborVnd} /
 * {@code priceMaterialsVnd} chỉ còn để đọc dữ liệu cũ — luồng mới đặt toàn bộ
 * giá vào {@code priceLaborVnd} và để vật tư bằng 0.
 *
 * <p>Domain model thuần (POJO), không phụ thuộc Spring/JPA/Lombok.
 */
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

    public Quotation() {
    }

    public BigDecimal totalPrice() {
        BigDecimal labor = priceLaborVnd != null ? priceLaborVnd : BigDecimal.ZERO;
        BigDecimal materials = priceMaterialsVnd != null ? priceMaterialsVnd : BigDecimal.ZERO;
        return labor.add(materials);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public Long getTechnicianId() {
        return technicianId;
    }

    public void setTechnicianId(Long technicianId) {
        this.technicianId = technicianId;
    }

    public String getSolution() {
        return solution;
    }

    public void setSolution(String solution) {
        this.solution = solution;
    }

    public BigDecimal getPriceLaborVnd() {
        return priceLaborVnd;
    }

    public void setPriceLaborVnd(BigDecimal priceLaborVnd) {
        this.priceLaborVnd = priceLaborVnd;
    }

    public BigDecimal getPriceMaterialsVnd() {
        return priceMaterialsVnd;
    }

    public void setPriceMaterialsVnd(BigDecimal priceMaterialsVnd) {
        this.priceMaterialsVnd = priceMaterialsVnd;
    }

    public LocalDateTime getInspectionTime() {
        return inspectionTime;
    }

    public void setInspectionTime(LocalDateTime inspectionTime) {
        this.inspectionTime = inspectionTime;
    }

    public LocalDateTime getEstimatedFinish() {
        return estimatedFinish;
    }

    public void setEstimatedFinish(LocalDateTime estimatedFinish) {
        this.estimatedFinish = estimatedFinish;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public QuotationStatus getStatus() {
        return status;
    }

    public void setStatus(QuotationStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }

    /** Builder viết tay thay cho Lombok @Builder. */
    public static final class Builder {
        private final Quotation instance = new Quotation();

        public Builder id(Long id) {
            instance.id = id;
            return this;
        }

        public Builder requestId(Long requestId) {
            instance.requestId = requestId;
            return this;
        }

        public Builder technicianId(Long technicianId) {
            instance.technicianId = technicianId;
            return this;
        }

        public Builder solution(String solution) {
            instance.solution = solution;
            return this;
        }

        public Builder priceLaborVnd(BigDecimal priceLaborVnd) {
            instance.priceLaborVnd = priceLaborVnd;
            return this;
        }

        public Builder priceMaterialsVnd(BigDecimal priceMaterialsVnd) {
            instance.priceMaterialsVnd = priceMaterialsVnd;
            return this;
        }

        public Builder inspectionTime(LocalDateTime inspectionTime) {
            instance.inspectionTime = inspectionTime;
            return this;
        }

        public Builder estimatedFinish(LocalDateTime estimatedFinish) {
            instance.estimatedFinish = estimatedFinish;
            return this;
        }

        public Builder note(String note) {
            instance.note = note;
            return this;
        }

        public Builder status(QuotationStatus status) {
            instance.status = status;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            instance.createdAt = createdAt;
            return this;
        }

        public Builder createdBy(Long createdBy) {
            instance.createdBy = createdBy;
            return this;
        }

        public Builder updatedAt(LocalDateTime updatedAt) {
            instance.updatedAt = updatedAt;
            return this;
        }

        public Builder updatedBy(Long updatedBy) {
            instance.updatedBy = updatedBy;
            return this;
        }

        public Quotation build() {
            return instance;
        }
    }
}
