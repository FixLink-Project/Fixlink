package com.fixlink.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO cho mỗi item trong danh sách user (RC-19) và chi tiết user (RC-20).
 * Bao gồm nested customerProfile hoặc technicianProfile tùy vai trò.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDto {

    private String id;
    private String username;
    private String role;
    private String status;
    private LocalDateTime createdAt;

    // Nested profile (chỉ 1 trong 2 xuất hiện tùy role)
    private CustomerProfileDto customerProfile;
    private TechnicianProfileDto technicianProfile;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
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
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TechnicianProfileDto {
        private String fullName;
        private String phone;
        private String email;
        private String citizenId;
        private String avatarUrl;
        private String verificationStatus;
        private Integer yearsExperience;
        private BigDecimal avgRating;
        private Integer completedJobs;
        private Long walletBalance;
        private Boolean isOnline;
    }
}
