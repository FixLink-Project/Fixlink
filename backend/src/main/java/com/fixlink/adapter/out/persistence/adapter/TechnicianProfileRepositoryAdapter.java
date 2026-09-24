package com.fixlink.adapter.out.persistence.adapter;

import com.fixlink.adapter.out.persistence.entity.TechnicianProfileJpaEntity;
import com.fixlink.adapter.out.persistence.entity.UserJpaEntity;
import com.fixlink.adapter.out.persistence.mapper.UserMapper;
import com.fixlink.adapter.out.persistence.repository.SpringDataTechnicianProfileRepository;
import com.fixlink.adapter.out.persistence.repository.SpringDataUserRepository;
import com.fixlink.application.port.out.TechnicianProfileRepositoryPort;
import com.fixlink.domain.model.TechnicianProfile;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class TechnicianProfileRepositoryAdapter implements TechnicianProfileRepositoryPort {

    private final SpringDataTechnicianProfileRepository springDataTechnicianProfileRepository;
    private final SpringDataUserRepository springDataUserRepository;
    private final UserMapper userMapper;

    public TechnicianProfileRepositoryAdapter(SpringDataTechnicianProfileRepository springDataTechnicianProfileRepository,
                                             SpringDataUserRepository springDataUserRepository,
                                             UserMapper userMapper) {
        this.springDataTechnicianProfileRepository = springDataTechnicianProfileRepository;
        this.springDataUserRepository = springDataUserRepository;
        this.userMapper = userMapper;
    }

    @Override
    public TechnicianProfile save(TechnicianProfile profile) {
        UserJpaEntity userEntity = springDataUserRepository.findById(profile.getUserId())
                .orElseThrow(() -> new IllegalStateException("User not found for ID: " + profile.getUserId()));

        TechnicianProfileJpaEntity entity = springDataTechnicianProfileRepository.findByUserIdAndDeletedAtIsNull(profile.getUserId())
                .orElseGet(() -> {
                    TechnicianProfileJpaEntity newEntity = new TechnicianProfileJpaEntity();
                    newEntity.setUser(userEntity);
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
        entity.setVerifiedBy(profile.getVerifiedBy());
        entity.setVerifiedAt(profile.getVerifiedAt());
        entity.setRejectionReason(profile.getRejectionReason());
        entity.setAvgRating(profile.getAvgRating());
        entity.setCompletedJobs(profile.getCompletedJobs());
        entity.setWalletBalance(profile.getWalletBalance());
        entity.setIsOnline(profile.getIsOnline());

        TechnicianProfileJpaEntity saved = springDataTechnicianProfileRepository.save(entity);
        return userMapper.toDomain(saved);
    }

    @Override
    public Optional<TechnicianProfile> findByUserId(Long userId) {
        return springDataTechnicianProfileRepository.findByUserIdAndDeletedAtIsNull(userId)
                .map(userMapper::toDomain);
    }

    @Override
    public boolean existsByPhoneOrEmailOrCitizenId(String phone, String email, String citizenId) {
        return springDataTechnicianProfileRepository.existsByPhoneOrEmailOrCitizenIdAndDeletedAtIsNull(phone, email, citizenId);
    }
}
