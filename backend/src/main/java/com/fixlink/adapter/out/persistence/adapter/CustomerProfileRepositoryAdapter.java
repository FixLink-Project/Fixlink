package com.fixlink.adapter.out.persistence.adapter;

import com.fixlink.adapter.out.persistence.entity.CustomerProfileJpaEntity;
import com.fixlink.adapter.out.persistence.mapper.UserMapper;
import com.fixlink.adapter.out.persistence.repository.SpringDataCustomerProfileRepository;
import com.fixlink.adapter.out.persistence.repository.SpringDataUserRepository;
import com.fixlink.application.port.out.CustomerProfileRepositoryPort;
import com.fixlink.domain.model.CustomerProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CustomerProfileRepositoryAdapter implements CustomerProfileRepositoryPort {

    private final SpringDataCustomerProfileRepository customerProfileRepository;
    private final SpringDataUserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public Optional<CustomerProfile> findByUserId(Long userId) {
        return customerProfileRepository.findByUserIdAndDeletedAtIsNull(userId)
                .map(userMapper::toDomain);
    }

    @Override
    public CustomerProfile save(CustomerProfile profile) {
        CustomerProfileJpaEntity entity;
        if (profile.getUserId() != null) {
            entity = customerProfileRepository.findByUserId(profile.getUserId())
                    .orElseGet(() -> {
                        CustomerProfileJpaEntity newEntity = userMapper.toJpaEntity(profile);
                        userRepository.findById(profile.getUserId()).ifPresent(newEntity::setUser);
                        return newEntity;
                    });
            entity.setFullName(profile.getFullName());
            entity.setPhone(profile.getPhone());
            entity.setEmail(profile.getEmail());
            entity.setAvatarUrl(profile.getAvatarUrl());
            entity.setMembershipTier(profile.getMembershipTier());
            if (entity.getUser() == null) {
                userRepository.findById(profile.getUserId()).ifPresent(entity::setUser);
            }
        } else {
            entity = userMapper.toJpaEntity(profile);
        }
        CustomerProfileJpaEntity saved = customerProfileRepository.save(entity);
        return userMapper.toDomain(saved);
    }

    @Override
    public boolean existsByPhoneOrEmail(String phone, String email) {
        return customerProfileRepository.existsByPhoneOrEmail(phone, email);
    }
}
