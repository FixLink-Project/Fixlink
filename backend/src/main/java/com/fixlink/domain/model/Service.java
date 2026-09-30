package com.fixlink.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Thực thể gói dịch vụ sửa chữa chi tiết.
 *
 * <p>Domain model thuần (POJO), không phụ thuộc Spring/JPA/Lombok.
 */
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

    public Service() {
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

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice;
    }

    public Integer getEstimatedDurationMinutes() {
        return estimatedDurationMinutes;
    }

    public void setEstimatedDurationMinutes(Integer estimatedDurationMinutes) {
        this.estimatedDurationMinutes = estimatedDurationMinutes;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
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

    /** Builder viết tay thay cho Lombok @Builder. */
    public static final class Builder {
        private final Service instance = new Service();

        public Builder id(Long id) {
            instance.id = id;
            return this;
        }

        public Builder categoryId(Long categoryId) {
            instance.categoryId = categoryId;
            return this;
        }

        public Builder code(String code) {
            instance.code = code;
            return this;
        }

        public Builder name(String name) {
            instance.name = name;
            return this;
        }

        public Builder description(String description) {
            instance.description = description;
            return this;
        }

        public Builder basePrice(BigDecimal basePrice) {
            instance.basePrice = basePrice;
            return this;
        }

        public Builder estimatedDurationMinutes(Integer estimatedDurationMinutes) {
            instance.estimatedDurationMinutes = estimatedDurationMinutes;
            return this;
        }

        public Builder isActive(Boolean isActive) {
            instance.isActive = isActive;
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

        public Service build() {
            return instance;
        }
    }
}
