package com.fixlink.application.port.out;

import com.fixlink.domain.model.TechnicianProfile;

import java.util.Optional;

public interface TechnicianProfileRepositoryPort {

    TechnicianProfile save(TechnicianProfile profile);

    Optional<TechnicianProfile> findByUserId(Long userId);

    boolean existsByPhoneOrEmailOrCitizenId(String phone, String email, String citizenId);
}
