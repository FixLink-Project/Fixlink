package com.fixlink.adapter.in.web.dto.response;

import com.fixlink.domain.model.VerificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechnicianProfileResponse {
    private Long userId;
    private String fullName;
    private String phone;
    private String email;
    private String avatarUrl;
    private String citizenId;
    private String idCardFrontUrl;
    private String idCardBackUrl;
    private String bio;
    private Integer yearsExperience;
    private VerificationStatus verificationStatus;
    private Boolean isOnline;
    private BigDecimal avgRating;
    private Integer completedJobs;
    private BigDecimal walletBalance;
    private List<CategorySimpleResponse> categories;
    private List<AreaSimpleResponse> areas;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
