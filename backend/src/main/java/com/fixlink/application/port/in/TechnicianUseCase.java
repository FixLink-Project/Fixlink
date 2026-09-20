package com.fixlink.application.port.in;

import com.fixlink.domain.model.TechnicianProfile;

import java.util.List;

public interface TechnicianUseCase {

    TechnicianProfile getMyProfile(Long userId);

    TechnicianProfile updateProfile(Long userId, UpdateTechnicianProfileCommand command);

    TechnicianProfile updateOnlineStatus(Long userId, boolean isOnline);

    List<RepairRequestDto> getMyRepairRequests(Long userId);

    record UpdateTechnicianProfileCommand(
            String fullName,
            String phone,
            String email,
            String bio,
            Integer yearsExperience,
            String avatarUrl
    ) {}

    record RepairRequestDto(
            String id,
            String customerName,
            String customerPhone,
            String serviceType,
            String address,
            String description,
            String status,
            String scheduledTime
    ) {}
}
