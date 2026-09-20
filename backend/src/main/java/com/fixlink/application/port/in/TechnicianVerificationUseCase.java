package com.fixlink.application.port.in;

import com.fixlink.domain.model.VerificationStatus;

import java.time.LocalDateTime;

public interface TechnicianVerificationUseCase {

    VerificationResult verifyTechnician(VerifyTechnicianCommand command);

    record VerifyTechnicianCommand(
            Long technicianUserId,
            VerificationStatus status,
            String note,
            String rejectionReason,
            Long adminUserId
    ) {}

    record VerificationResult(
            String userId,
            VerificationStatus verificationStatus,
            LocalDateTime verifiedAt,
            String verifiedBy
    ) {}
}
