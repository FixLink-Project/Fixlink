package com.fixlink.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Domain model thuần (POJO) của yêu cầu sửa chữa.
 *
 * <p>Theo kiến trúc Hexagonal, lớp domain KHÔNG phụ thuộc Spring, JPA hay Lombok.
 * Getter/setter/builder được viết tay để tầng domain không kéo theo annotation
 * processor của framework nào.
 */
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
    private LocalDateTime applyDeadline;
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

    public RepairRequest() {
    }

    public boolean canEdit() {
        return status == RequestStatus.DRAFT || status == RequestStatus.OPEN;
    }

    public boolean canCancel() {
        return status != RequestStatus.IN_PROGRESS
                && status != RequestStatus.AWAITING_ACCEPTANCE
                && status != RequestStatus.COMPLETED
                && status != RequestStatus.CANCELLED;
    }

    public boolean isBiddingExpired() {
        return applyDeadline != null && LocalDateTime.now().isAfter(applyDeadline);
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

    public String getRequestCode() {
        return requestCode;
    }

    public void setRequestCode(String requestCode) {
        this.requestCode = requestCode;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Long getTechnicianId() {
        return technicianId;
    }

    public void setTechnicianId(Long technicianId) {
        this.technicianId = technicianId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getServiceId() {
        return serviceId;
    }

    public void setServiceId(Long serviceId) {
        this.serviceId = serviceId;
    }

    public Long getAreaId() {
        return areaId;
    }

    public void setAreaId(Long areaId) {
        this.areaId = areaId;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public LocalDateTime getRequestedTime() {
        return requestedTime;
    }

    public void setRequestedTime(LocalDateTime requestedTime) {
        this.requestedTime = requestedTime;
    }

    public BigDecimal getAgreedPrice() {
        return agreedPrice;
    }

    public void setAgreedPrice(BigDecimal agreedPrice) {
        this.agreedPrice = agreedPrice;
    }

    public BigDecimal getDepositAmount() {
        return depositAmount;
    }

    public void setDepositAmount(BigDecimal depositAmount) {
        this.depositAmount = depositAmount;
    }

    public BigDecimal getBudgetRef() {
        return budgetRef;
    }

    public void setBudgetRef(BigDecimal budgetRef) {
        this.budgetRef = budgetRef;
    }

    public LocalDateTime getApplyDeadline() {
        return applyDeadline;
    }

    public void setApplyDeadline(LocalDateTime applyDeadline) {
        this.applyDeadline = applyDeadline;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public Long getSelectedQuotationId() {
        return selectedQuotationId;
    }

    public void setSelectedQuotationId(Long selectedQuotationId) {
        this.selectedQuotationId = selectedQuotationId;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
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

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public Long getDeletedBy() {
        return deletedBy;
    }

    public void setDeletedBy(Long deletedBy) {
        this.deletedBy = deletedBy;
    }

    public List<String> getMediaUrls() {
        return mediaUrls;
    }

    public void setMediaUrls(List<String> mediaUrls) {
        this.mediaUrls = mediaUrls;
    }

    /** Builder viết tay thay cho Lombok @Builder. */
    public static final class Builder {
        private final RepairRequest instance = new RepairRequest();

        public Builder id(Long id) {
            instance.id = id;
            return this;
        }

        public Builder requestCode(String requestCode) {
            instance.requestCode = requestCode;
            return this;
        }

        public Builder customerId(Long customerId) {
            instance.customerId = customerId;
            return this;
        }

        public Builder technicianId(Long technicianId) {
            instance.technicianId = technicianId;
            return this;
        }

        public Builder categoryId(Long categoryId) {
            instance.categoryId = categoryId;
            return this;
        }

        public Builder serviceId(Long serviceId) {
            instance.serviceId = serviceId;
            return this;
        }

        public Builder areaId(Long areaId) {
            instance.areaId = areaId;
            return this;
        }

        public Builder status(RequestStatus status) {
            instance.status = status;
            return this;
        }

        public Builder title(String title) {
            instance.title = title;
            return this;
        }

        public Builder description(String description) {
            instance.description = description;
            return this;
        }

        public Builder address(String address) {
            instance.address = address;
            return this;
        }

        public Builder latitude(BigDecimal latitude) {
            instance.latitude = latitude;
            return this;
        }

        public Builder longitude(BigDecimal longitude) {
            instance.longitude = longitude;
            return this;
        }

        public Builder requestedTime(LocalDateTime requestedTime) {
            instance.requestedTime = requestedTime;
            return this;
        }

        public Builder agreedPrice(BigDecimal agreedPrice) {
            instance.agreedPrice = agreedPrice;
            return this;
        }

        public Builder depositAmount(BigDecimal depositAmount) {
            instance.depositAmount = depositAmount;
            return this;
        }

        public Builder budgetRef(BigDecimal budgetRef) {
            instance.budgetRef = budgetRef;
            return this;
        }

        public Builder applyDeadline(LocalDateTime applyDeadline) {
            instance.applyDeadline = applyDeadline;
            return this;
        }

        public Builder cancelReason(String cancelReason) {
            instance.cancelReason = cancelReason;
            return this;
        }

        public Builder selectedQuotationId(Long selectedQuotationId) {
            instance.selectedQuotationId = selectedQuotationId;
            return this;
        }

        public Builder version(Long version) {
            instance.version = version;
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

        public Builder deletedAt(LocalDateTime deletedAt) {
            instance.deletedAt = deletedAt;
            return this;
        }

        public Builder deletedBy(Long deletedBy) {
            instance.deletedBy = deletedBy;
            return this;
        }

        public Builder mediaUrls(List<String> mediaUrls) {
            instance.mediaUrls = mediaUrls;
            return this;
        }

        public RepairRequest build() {
            return instance;
        }
    }
}
