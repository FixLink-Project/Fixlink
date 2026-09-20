package com.fixlink.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Thực thể lưu lịch sử xét duyệt hồ sơ eKYC CCCD của thợ (Jira RC-8).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
}
