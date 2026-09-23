package com.fixlink.adapter.out.persistence.adapter;

import com.fixlink.adapter.out.persistence.entity.TechnicianProfileJpaEntity;
import com.fixlink.adapter.out.persistence.mapper.UserMapper;
import com.fixlink.adapter.out.persistence.repository.SpringDataTechnicianProfileRepository;
import com.fixlink.adapter.out.persistence.repository.SpringDataUserRepository;
import com.fixlink.application.port.out.TechnicianProfileRepositoryPort;
import com.fixlink.domain.model.TechnicianProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TechnicianProfileRepositoryAdapter implements TechnicianProfileRepositoryPort {

    private final SpringDataTechnicianProfileRepository technicianProfileRepository;
    private final SpringDataUserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public Optional<TechnicianProfile> findByUserId(Long userId) {
        return technicianProfileRepository.findByUserIdAndDeletedAtIsNull(userId)
                .map(userMapper::toDomain);
    }

    @Override
    public TechnicianProfile save(TechnicianProfile profile) {
        TechnicianProfileJpaEntity entity;
        if (profile.getUserId() != null) {
            entity = technicianProfileRepository.findByUserId(profile.getUserId())
                    .orElseGet(() -> {
                        TechnicianProfileJpaEntity newEntity = userMapper.toJpaEntity(profile);
                        userRepository.findById(profile.getUserId()).ifPresent(newEntity::setUser);
                        return newEntity;
                    });
            entity.setFullName(profile.getFullName());
            entity.setPhone(profile.getPhone());
            entity.setEmail(profile.getEmail());
            entity.setAvatarUrl(profile.getAvatarUrl());
            entity.setCitizenId(profile.getCitizenId());
            entity.setIdCardFrontUrl(profile.getIdCardFrontUrl());
            entity.setIdCardBackUrl(profile.getIdCardBackUrl());
            entity.setBio(profile.getBio());
            entity.setYearsExperience(profile.getYearsExperience());
            entity.setVerificationStatus(profile.getVerificationStatus());
            entity.setVerifiedBy(profile.getVerifiedBy() != null ? String.valueOf(profile.getVerifiedBy()) : null);
            entity.setVerifiedAt(profile.getVerifiedAt());
            entity.setRejectionReason(profile.getRejectionReason());
            entity.setAvgRating(profile.getAvgRating());
            entity.setCompletedJobs(profile.getCompletedJobs());
            entity.setWalletBalance(profile.getWalletBalance());
            entity.setIsOnline(profile.getIsOnline());
            if (entity.getUser() == null) {
                userRepository.findById(profile.getUserId()).ifPresent(entity::setUser);
            }
        } else {
            entity = userMapper.toJpaEntity(profile);
        }
        TechnicianProfileJpaEntity saved = technicianProfileRepository.save(entity);
        return userMapper.toDomain(saved);
    }

    @Override
    public boolean existsByPhoneOrEmailOrCitizenId(String phone, String email, String citizenId) {
        return technicianProfileRepository.existsByPhoneOrEmailOrCitizenId(phone, email, citizenId);
    }
}
