package com.fixlink.application.port.out;

import com.fixlink.domain.model.CustomerProfile;

import java.util.Optional;

public interface CustomerProfileRepositoryPort {

    CustomerProfile save(CustomerProfile profile);

    Optional<CustomerProfile> findByUserId(Long userId);

    boolean existsByPhoneOrEmail(String phone, String email);
}
