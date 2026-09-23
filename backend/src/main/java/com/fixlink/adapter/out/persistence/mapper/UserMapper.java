package com.fixlink.adapter.out.persistence.mapper;

import com.fixlink.adapter.out.persistence.entity.CustomerProfileJpaEntity;
import com.fixlink.adapter.out.persistence.entity.TechnicianProfileJpaEntity;
import com.fixlink.adapter.out.persistence.entity.UserJpaEntity;
import com.fixlink.domain.model.CustomerProfile;
import com.fixlink.domain.model.TechnicianProfile;
import com.fixlink.domain.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public CustomerProfile toDomain(CustomerProfileJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new CustomerProfile(
                entity.getUserId(),
                entity.getFullName(),
                entity.getPhone(),
                entity.getEmail(),
                entity.getAvatarUrl(),
                entity.getMembershipTier(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public CustomerProfileJpaEntity toJpaEntity(CustomerProfile domain) {
        if (domain == null) {
            return null;
        }
        CustomerProfileJpaEntity entity = new CustomerProfileJpaEntity();
        entity.setUserId(domain.getUserId());
        entity.setFullName(domain.getFullName());
        entity.setPhone(domain.getPhone());
        entity.setEmail(domain.getEmail());
        entity.setAvatarUrl(domain.getAvatarUrl());
        entity.setMembershipTier(domain.getMembershipTier());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }

    public User toDomain(UserJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new User(
                entity.getId(),
                entity.getUsername(),
                entity.getPasswordHash(),
                entity.getRole(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public UserJpaEntity toJpaEntity(User domain) {
        if (domain == null) {
            return null;
        }
        UserJpaEntity entity = new UserJpaEntity();
        entity.setId(domain.getId());
        entity.setUsername(domain.getUsername());
        entity.setPasswordHash(domain.getPasswordHash());
        entity.setRole(domain.getRole());
        entity.setStatus(domain.getStatus());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }

    public TechnicianProfile toDomain(TechnicianProfileJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        TechnicianProfile domain = new TechnicianProfile();
        domain.setUserId(entity.getUserId());
        domain.setFullName(entity.getFullName());
        domain.setPhone(entity.getPhone());
        domain.setEmail(entity.getEmail());
        domain.setAvatarUrl(entity.getAvatarUrl());
        domain.setCitizenId(entity.getCitizenId());
        domain.setIdCardFrontUrl(entity.getIdCardFrontUrl());
        domain.setIdCardBackUrl(entity.getIdCardBackUrl());
        domain.setBio(entity.getBio());
        domain.setYearsExperience(entity.getYearsExperience());
        domain.setVerificationStatus(entity.getVerificationStatus());
        if (entity.getVerifiedBy() != null && entity.getVerifiedBy().matches("\\d+")) {
            domain.setVerifiedBy(Long.parseLong(entity.getVerifiedBy()));
        }
        domain.setVerifiedAt(entity.getVerifiedAt());
        domain.setRejectionReason(entity.getRejectionReason());
        domain.setAvgRating(entity.getAvgRating());
        domain.setCompletedJobs(entity.getCompletedJobs());
        domain.setWalletBalance(entity.getWalletBalance());
        domain.setIsOnline(entity.getIsOnline());
        domain.setCreatedAt(entity.getCreatedAt());
        domain.setUpdatedAt(entity.getUpdatedAt());
        return domain;
    }

    public TechnicianProfileJpaEntity toJpaEntity(TechnicianProfile domain) {
        if (domain == null) {
            return null;
        }
        TechnicianProfileJpaEntity entity = new TechnicianProfileJpaEntity();
        entity.setUserId(domain.getUserId());
        entity.setFullName(domain.getFullName());
        entity.setPhone(domain.getPhone());
        entity.setEmail(domain.getEmail());
        entity.setAvatarUrl(domain.getAvatarUrl());
        entity.setCitizenId(domain.getCitizenId());
        entity.setIdCardFrontUrl(domain.getIdCardFrontUrl());
        entity.setIdCardBackUrl(domain.getIdCardBackUrl());
        entity.setBio(domain.getBio());
        entity.setYearsExperience(domain.getYearsExperience());
        entity.setVerificationStatus(domain.getVerificationStatus());
        entity.setVerifiedBy(domain.getVerifiedBy() != null ? String.valueOf(domain.getVerifiedBy()) : null);
        entity.setVerifiedAt(domain.getVerifiedAt());
        entity.setRejectionReason(domain.getRejectionReason());
        entity.setAvgRating(domain.getAvgRating());
        entity.setCompletedJobs(domain.getCompletedJobs());
        entity.setWalletBalance(domain.getWalletBalance());
        entity.setIsOnline(domain.getIsOnline());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}
