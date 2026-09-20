package com.fixlink.adapter.in.web.dto.response;

import com.fixlink.domain.model.CustomerProfile;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Thông tin hồ sơ khách hàng")
public class CustomerProfileResponseDto {

    private Long userId;
    private String fullName;
    private String phone;
    private String email;
    private String avatarUrl;
    private String membershipTier;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CustomerProfileResponseDto fromDomain(CustomerProfile profile) {
        if (profile == null) {
            return null;
        }
        return CustomerProfileResponseDto.builder()
                .userId(profile.getUserId())
                .fullName(profile.getFullName())
                .phone(profile.getPhone())
                .email(profile.getEmail())
                .avatarUrl(profile.getAvatarUrl())
                .membershipTier(profile.getMembershipTier())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}
