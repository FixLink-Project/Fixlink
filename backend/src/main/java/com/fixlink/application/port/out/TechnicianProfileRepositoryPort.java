package com.fixlink.application.port.out;

import com.fixlink.domain.model.TechnicianProfile;

import java.util.Optional;

public interface TechnicianProfileRepositoryPort {
    Optional<TechnicianProfile> findByUserId(Long userId);
    TechnicianProfile save(TechnicianProfile profile);
    boolean existsByPhoneOrEmailOrCitizenId(String phone, String email, String citizenId);
}
