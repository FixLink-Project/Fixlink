package com.fixlink.application.port.in;

import com.fixlink.domain.model.Role;
import com.fixlink.domain.model.UserStatus;
import com.fixlink.domain.model.VerificationStatus;

import java.time.LocalDateTime;

public interface AuthUseCase {

    AuthResult login(LoginCommand command);

    CustomerRegisterResult registerCustomer(RegisterCustomerCommand command);

    TechnicianRegisterResult registerTechnician(RegisterTechnicianCommand command);

    record LoginCommand(String username, String password, String deviceToken) {}

    record RegisterCustomerCommand(String username, String password, String fullName, String phone, String email) {}

    record RegisterTechnicianCommand(
            String username,
            String password,
            String fullName,
            String phone,
            String email,
            String citizenId,
            String idCardFrontUrl,
            String idCardBackUrl,
            String bio,
            Integer yearsExperience
    ) {}

    record AuthResult(
            String accessToken,
            String refreshToken,
            String tokenType,
            long expiresIn,
            UserInfo user
    ) {}

    record UserInfo(
            String id,
            String username,
            Role role,
            UserStatus status,
            String fullName,
            String avatarUrl,
            boolean isVerified,
            VerificationStatus verificationStatus
    ) {}

    record CustomerRegisterResult(
            String userId,
            String username,
            Role role,
            String fullName,
            String phone,
            String email,
            LocalDateTime createdAt
    ) {}

    record TechnicianRegisterResult(
            String userId,
            String username,
            Role role,
            VerificationStatus verificationStatus,
            LocalDateTime createdAt
    ) {}
}
