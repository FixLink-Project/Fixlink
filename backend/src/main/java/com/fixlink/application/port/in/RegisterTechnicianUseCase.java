package com.fixlink.application.port.in;

import com.fixlink.domain.model.Role;
import com.fixlink.domain.model.UserStatus;
import com.fixlink.domain.model.VerificationStatus;
import java.time.LocalDateTime;

public interface RegisterTechnicianUseCase {

    record RegisterTechnicianCommand(
            String username,
            String password,
            String fullName,
            String phone,
            String email,
            String address,
            String specialization,
            String citizenId,
            String idCardFrontUrl,
            String idCardBackUrl,
            String bio,
            Integer yearsExperience
    ) {}

    record TechnicianRegisterResult(
            String userId,
            String username,
            Role role,
            UserStatus status,
            VerificationStatus verificationStatus,
            LocalDateTime createdAt
    ) {}

    TechnicianRegisterResult registerTechnician(RegisterTechnicianCommand command);
}
