package com.fixlink.application.port.in;

import com.fixlink.domain.model.CustomerProfile;

import java.time.LocalDateTime;
import java.util.List;

public interface CustomerProfileUseCase {

    CustomerProfile getProfile(Long userId);

    CustomerProfile updateProfile(Long targetUserId, Long currentUserId, UpdateProfileCommand command);

    List<AuditLogEntry> getAuditTrail(Long userId);

    record UpdateProfileCommand(
            String fullName,
            String phone,
            String email,
            String avatarUrl
    ) {}

    record AuditLogEntry(
            Long id,
            Long userId,
            String action,
            String entityName,
            String entityId,
            String oldValues,
            String newValues,
            LocalDateTime createdAt
    ) {}
}
