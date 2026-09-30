package com.fixlink.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Thực thể danh mục ngành nghề dịch vụ sửa chữa.
 *
 * <p>Domain model thuần (POJO), không phụ thuộc Spring/JPA/Lombok.
 */
public class ServiceCategory {
    private Long id;
    private String code;
    private String name;
    private String description;
    private String iconUrl;
    private Integer displayOrder;
    private Boolean isActive;
    private List<Service> services = new ArrayList<>();
    private LocalDateTime createdAt;
    private Long createdBy;
    private LocalDateTime updatedAt;
    private Long updatedBy;
    private LocalDateTime deletedAt;
    private Long deletedBy;

    public ServiceCategory() {
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

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public List<Service> getServices() {
        return services;
    }

    public void setServices(List<Service> services) {
        this.services = services;
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
        private final ServiceCategory instance = new ServiceCategory();

        public Builder id(Long id) {
            instance.id = id;
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

        public Builder iconUrl(String iconUrl) {
            instance.iconUrl = iconUrl;
            return this;
        }

        public Builder displayOrder(Integer displayOrder) {
            instance.displayOrder = displayOrder;
            return this;
        }

        public Builder isActive(Boolean isActive) {
            instance.isActive = isActive;
            return this;
        }

        public Builder services(List<Service> services) {
            instance.services = services;
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

        public ServiceCategory build() {
            return instance;
        }
    }
}
