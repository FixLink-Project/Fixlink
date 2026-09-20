package com.fixlink.domain.model;

import java.time.LocalDateTime;

/**
 * Domain entity CustomerProfile - Hồ sơ khách hàng (thuần Java)
 */
public class CustomerProfile {
    private Long userId;
    private String fullName;
    private String phone;
    private String email;
    private String avatarUrl;
    private String membershipTier;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public CustomerProfile() {
    }

    public CustomerProfile(Long userId, String fullName, String phone, String email,
                           String avatarUrl, String membershipTier,
                           LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.userId = userId;
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.avatarUrl = avatarUrl;
        this.membershipTier = membershipTier != null ? membershipTier : "STANDARD";
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public String getMembershipTier() {
        return membershipTier;
    }

    public void setMembershipTier(String membershipTier) {
        this.membershipTier = membershipTier;
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
