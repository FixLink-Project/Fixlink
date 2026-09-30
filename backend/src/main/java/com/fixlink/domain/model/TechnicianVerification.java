package com.fixlink.domain.model;

import java.time.LocalDateTime;

/**
 * Thực thể lưu lịch sử xét duyệt hồ sơ eKYC CCCD của thợ.
 *
 * <p>Domain model thuần (POJO), không phụ thuộc Spring/JPA/Lombok.
 */
public class TechnicianVerification {
    private Long id;
    private Long technicianId;
    private Long adminId;
    private VerificationStatus status;
    private String citizenId;
    private String idCardFrontUrl;
    private String idCardBackUrl;
    private String rejectionReason;
    private String notes;
    private LocalDateTime verifiedAt;
    private LocalDateTime createdAt;
    private Long createdBy;
    private LocalDateTime updatedAt;
    private Long updatedBy;
    private LocalDateTime deletedAt;
    private Long deletedBy;

    public TechnicianVerification() {
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

    public Long getTechnicianId() {
        return technicianId;
    }

    public void setTechnicianId(Long technicianId) {
        this.technicianId = technicianId;
    }

    public Long getAdminId() {
        return adminId;
    }

    public void setAdminId(Long adminId) {
        this.adminId = adminId;
    }

    public VerificationStatus getStatus() {
        return status;
    }

    public void setStatus(VerificationStatus status) {
        this.status = status;
    }

    public String getCitizenId() {
        return citizenId;
    }

    public void setCitizenId(String citizenId) {
        this.citizenId = citizenId;
    }

    public String getIdCardFrontUrl() {
        return idCardFrontUrl;
    }

    public void setIdCardFrontUrl(String idCardFrontUrl) {
        this.idCardFrontUrl = idCardFrontUrl;
    }

    public String getIdCardBackUrl() {
        return idCardBackUrl;
    }

    public void setIdCardBackUrl(String idCardBackUrl) {
        this.idCardBackUrl = idCardBackUrl;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
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
        private final TechnicianVerification instance = new TechnicianVerification();

        public Builder id(Long id) {
            instance.id = id;
            return this;
        }

        public Builder technicianId(Long technicianId) {
            instance.technicianId = technicianId;
            return this;
        }

        public Builder adminId(Long adminId) {
            instance.adminId = adminId;
            return this;
        }

        public Builder status(VerificationStatus status) {
            instance.status = status;
            return this;
        }

        public Builder citizenId(String citizenId) {
            instance.citizenId = citizenId;
            return this;
        }

        public Builder idCardFrontUrl(String idCardFrontUrl) {
            instance.idCardFrontUrl = idCardFrontUrl;
            return this;
        }

        public Builder idCardBackUrl(String idCardBackUrl) {
            instance.idCardBackUrl = idCardBackUrl;
            return this;
        }

        public Builder rejectionReason(String rejectionReason) {
            instance.rejectionReason = rejectionReason;
            return this;
        }

        public Builder notes(String notes) {
            instance.notes = notes;
            return this;
        }

        public Builder verifiedAt(LocalDateTime verifiedAt) {
            instance.verifiedAt = verifiedAt;
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

        public TechnicianVerification build() {
            return instance;
        }
    }
}
