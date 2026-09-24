package com.fixlink.adapter.in.web.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fixlink.domain.model.Role;
import com.fixlink.domain.model.UserStatus;
import com.fixlink.domain.model.VerificationStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponseDto {
    private String id;
    private String username;
    private Role role;
    private UserStatus status;
    private LocalDateTime createdAt;
    private CustomerProfileDto customerProfile;
    private TechnicianProfileDto technicianProfile;

    public static UserResponseDto fromDomain(com.fixlink.domain.model.User user) {
        if (user == null) {
            return null;
        }
        UserResponseDto dto = new UserResponseDto();
        dto.setId("usr_" + user.getId());
        dto.setUsername(user.getUsername());
        dto.setRole(user.getRole());
        dto.setStatus(user.getStatus());
        dto.setCreatedAt(user.getCreatedAt());

        if (user.getCustomerProfile() != null) {
            CustomerProfileDto cDto = new CustomerProfileDto();
            cDto.setFullName(user.getCustomerProfile().getFullName());
            cDto.setPhone(user.getCustomerProfile().getPhone());
            cDto.setEmail(user.getCustomerProfile().getEmail());
            cDto.setAvatarUrl(user.getCustomerProfile().getAvatarUrl());
            cDto.setMembershipTier(user.getCustomerProfile().getMembershipTier());
            dto.setCustomerProfile(cDto);
        }

        if (user.getTechnicianProfile() != null) {
            TechnicianProfileDto tDto = new TechnicianProfileDto();
            tDto.setFullName(user.getTechnicianProfile().getFullName());
            tDto.setPhone(user.getTechnicianProfile().getPhone());
            tDto.setEmail(user.getTechnicianProfile().getEmail());
            tDto.setCitizenId(user.getTechnicianProfile().getCitizenId());
            tDto.setAvatarUrl(user.getTechnicianProfile().getAvatarUrl());
            tDto.setVerificationStatus(user.getTechnicianProfile().getVerificationStatus());
            tDto.setYearsExperience(user.getTechnicianProfile().getYearsExperience());
            tDto.setAvgRating(user.getTechnicianProfile().getAvgRating());
            tDto.setCompletedJobs(user.getTechnicianProfile().getCompletedJobs());
            tDto.setWalletBalance(user.getTechnicianProfile().getWalletBalance());
            tDto.setIsOnline(user.getTechnicianProfile().getIsOnline());
            tDto.setIdCardFrontUrl(user.getTechnicianProfile().getIdCardFrontUrl());
            tDto.setIdCardBackUrl(user.getTechnicianProfile().getIdCardBackUrl());
            tDto.setBio(user.getTechnicianProfile().getBio());
            dto.setTechnicianProfile(tDto);
        }

        return dto;
    }

    @Getter
    @Setter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CustomerProfileDto {
        private String fullName;
        private String phone;
        private String email;
        private String avatarUrl;
        private String membershipTier;
    }

    @Getter
    @Setter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TechnicianProfileDto {
        private String fullName;
        private String phone;
        private String email;
        private String citizenId;
        private String avatarUrl;
        private VerificationStatus verificationStatus;
        private Integer yearsExperience;
        private BigDecimal avgRating;
        private Integer completedJobs;
        private BigDecimal walletBalance;
        private Boolean isOnline;
        private String idCardFrontUrl;
        private String idCardBackUrl;
        private String bio;
    }
}
