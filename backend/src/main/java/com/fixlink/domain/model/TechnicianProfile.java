package com.fixlink.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Domain entity TechnicianProfile - Hồ sơ kỹ thuật viên/thợ (thuần Java)
 */
public class TechnicianProfile {
    private Long userId;
    private String fullName;
    private String phone;
    private String email;
    private String avatarUrl;
    private String citizenId;
    private String idCardFrontUrl;
    private String idCardBackUrl;
    private String bio;
    private Integer yearsExperience;
    private VerificationStatus verificationStatus;
    private Long verifiedBy;
    private LocalDateTime verifiedAt;
    private String rejectionReason;
    private BigDecimal avgRating;
    private Integer completedJobs;
    private BigDecimal walletBalance;
    private Boolean isOnline;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public TechnicianProfile() {
        this.verificationStatus = VerificationStatus.PENDING;
        this.avgRating = BigDecimal.ZERO;
        this.completedJobs = 0;
        this.walletBalance = BigDecimal.ZERO;
        this.isOnline = false;
    }

    public void verify(Long adminUserId) {
        this.verificationStatus = VerificationStatus.APPROVED;
        this.verifiedBy = adminUserId;
        this.verifiedAt = LocalDateTime.now();
        this.rejectionReason = null;
    }

    public void reject(Long adminUserId, String reason) {
        this.verificationStatus = VerificationStatus.REJECTED;
        this.verifiedBy = adminUserId;
        this.verifiedAt = LocalDateTime.now();
        this.rejectionReason = reason;
    }

    // Getters and Setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
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

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public Integer getYearsExperience() {
        return yearsExperience;
    }

    public void setYearsExperience(Integer yearsExperience) {
        this.yearsExperience = yearsExperience;
    }

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(VerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public Long getVerifiedBy() {
        return verifiedBy;
    }

    public void setVerifiedBy(Long verifiedBy) {
        this.verifiedBy = verifiedBy;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public BigDecimal getAvgRating() {
        return avgRating;
    }

    public void setAvgRating(BigDecimal avgRating) {
        this.avgRating = avgRating;
    }

    public Integer getCompletedJobs() {
        return completedJobs;
    }

    public void setCompletedJobs(Integer completedJobs) {
        this.completedJobs = completedJobs;
    }

    public BigDecimal getWalletBalance() {
        return walletBalance;
    }

    public void setWalletBalance(BigDecimal walletBalance) {
        this.walletBalance = walletBalance;
    }

    public Boolean getIsOnline() {
        return isOnline;
    }

    public void setIsOnline(Boolean online) {
        isOnline = online;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
