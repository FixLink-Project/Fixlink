package com.fixlink.domain.model;

import java.time.LocalDateTime;

/**
 * Thực thể tệp đính kèm (media) - metadata của tệp đã upload lên Firebase Storage.
 * Dùng để lưu ảnh hiện trường trước/sau sửa chữa của một yêu cầu sửa chữa.
 *
 * <p>Domain model thuần (POJO), không phụ thuộc Spring/JPA/Lombok.
 */
public class Media {

    /** Giá trị mặc định cho cột owner_type khi tệp thuộc về một yêu cầu sửa chữa. */
    public static final String OWNER_REPAIR_REQUEST = "REPAIR_REQUEST";

    private Long id;
    private String ownerType;
    private Long ownerId;
    private MediaType mediaType;
    private String url;
    private Long uploadedBy;
    private Long createdBy;
    private LocalDateTime createdAt;

    public Media() {
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

    public String getOwnerType() {
        return ownerType;
    }

    public void setOwnerType(String ownerType) {
        this.ownerType = ownerType;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public void setMediaType(MediaType mediaType) {
        this.mediaType = mediaType;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Long getUploadedBy() {
        return uploadedBy != null ? uploadedBy : createdBy;
    }

    public void setUploadedBy(Long uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public Long getCreatedBy() {
        return createdBy != null ? createdBy : uploadedBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /** Builder viết tay thay cho Lombok @Builder. */
    public static final class Builder {
        private final Media instance = new Media();

        public Builder id(Long id) {
            instance.id = id;
            return this;
        }

        public Builder ownerType(String ownerType) {
            instance.ownerType = ownerType;
            return this;
        }

        public Builder ownerId(Long ownerId) {
            instance.ownerId = ownerId;
            return this;
        }

        public Builder mediaType(MediaType mediaType) {
            instance.mediaType = mediaType;
            return this;
        }

        public Builder url(String url) {
            instance.url = url;
            return this;
        }

        public Builder uploadedBy(Long uploadedBy) {
            instance.uploadedBy = uploadedBy;
            return this;
        }

        public Builder createdBy(Long createdBy) {
            instance.createdBy = createdBy;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            instance.createdAt = createdAt;
            return this;
        }

        public Media build() {
            return instance;
        }
    }
}
