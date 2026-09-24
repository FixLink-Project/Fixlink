package com.fixlink.application.port.out;

import com.fixlink.domain.model.CustomerProfile;

import java.util.Optional;

public interface CustomerProfileRepositoryPort {
    Optional<CustomerProfile> findByUserId(Long userId);
    CustomerProfile save(CustomerProfile profile);
    boolean existsByPhoneOrEmail(String phone, String email);
}
