package com.fixlink.adapter.out.persistence.adapter;

import com.fixlink.adapter.out.persistence.entity.CustomerProfileJpaEntity;
import com.fixlink.adapter.out.persistence.entity.UserJpaEntity;
import com.fixlink.adapter.out.persistence.mapper.UserMapper;
import com.fixlink.adapter.out.persistence.repository.SpringDataCustomerProfileRepository;
import com.fixlink.adapter.out.persistence.repository.SpringDataUserRepository;
import com.fixlink.application.port.out.CustomerProfileRepositoryPort;
import com.fixlink.domain.model.CustomerProfile;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CustomerProfileRepositoryAdapter implements CustomerProfileRepositoryPort {

    private final SpringDataCustomerProfileRepository springDataCustomerProfileRepository;
    private final SpringDataUserRepository springDataUserRepository;
    private final UserMapper userMapper;

    public CustomerProfileRepositoryAdapter(SpringDataCustomerProfileRepository springDataCustomerProfileRepository,
                                           SpringDataUserRepository springDataUserRepository,
                                           UserMapper userMapper) {
        this.springDataCustomerProfileRepository = springDataCustomerProfileRepository;
        this.springDataUserRepository = springDataUserRepository;
        this.userMapper = userMapper;
    }

    @Override
    public CustomerProfile save(CustomerProfile profile) {
        UserJpaEntity userEntity = springDataUserRepository.findById(profile.getUserId())
                .orElseThrow(() -> new IllegalStateException("User not found for ID: " + profile.getUserId()));

        CustomerProfileJpaEntity entity = springDataCustomerProfileRepository.findByUserIdAndDeletedAtIsNull(profile.getUserId())
                .orElseGet(() -> {
                    CustomerProfileJpaEntity newEntity = new CustomerProfileJpaEntity();
                    newEntity.setUser(userEntity);
                    return newEntity;
                });

        entity.setFullName(profile.getFullName());
        entity.setPhone(profile.getPhone());
        entity.setEmail(profile.getEmail());
        entity.setAvatarUrl(profile.getAvatarUrl());
        entity.setMembershipTier(profile.getMembershipTier());

        CustomerProfileJpaEntity saved = springDataCustomerProfileRepository.save(entity);
        return userMapper.toDomain(saved);
    }

    @Override
    public Optional<CustomerProfile> findByUserId(Long userId) {
        return springDataCustomerProfileRepository.findByUserIdAndDeletedAtIsNull(userId)
                .map(userMapper::toDomain);
    }

    @Override
    public boolean existsByPhoneOrEmail(String phone, String email) {
        return springDataCustomerProfileRepository.existsByPhoneOrEmailAndDeletedAtIsNull(phone, email);
    }
}
