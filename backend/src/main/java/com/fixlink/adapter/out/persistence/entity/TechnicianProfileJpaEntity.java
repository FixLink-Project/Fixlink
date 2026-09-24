package com.fixlink.adapter.out.persistence.entity;

import com.fixlink.domain.model.VerificationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "technician_profiles")
@Getter
@Setter
@NoArgsConstructor
public class TechnicianProfileJpaEntity extends BaseEntity {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private UserJpaEntity user;

    /** Nhóm việc thợ nhận, lưu ở bảng nối technician_categories. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "technician_categories",
            joinColumns = @JoinColumn(name = "technician_id")
    )
    @Column(name = "category_id", nullable = false)
    private Set<Long> categoryIds = new LinkedHashSet<>();

    /** Địa bàn thợ tới được, lưu ở bảng nối technician_areas. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "technician_areas",
            joinColumns = @JoinColumn(name = "technician_id")
    )
    @Column(name = "area_id", nullable = false)
    private Set<Long> areaIds = new LinkedHashSet<>();

    @Column(name = "full_name", length = 100, nullable = false)
    private String fullName;

    @Column(name = "phone", length = 20, nullable = false, unique = true)
    private String phone;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "citizen_id", length = 20, nullable = false, unique = true)
    private String citizenId;

    @Column(name = "id_card_front_url", length = 500)
    private String idCardFrontUrl;

    @Column(name = "id_card_back_url", length = 500)
    private String idCardBackUrl;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "years_experience")
    private Integer yearsExperience = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", length = 20, nullable = false)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    @Column(name = "verified_by")
    private Long verifiedBy;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "rejection_reason", length = 255)
    private String rejectionReason;

    @Column(name = "avg_rating", precision = 3, scale = 2)
    private BigDecimal avgRating = BigDecimal.ZERO;

    @Column(name = "completed_jobs")
    private Integer completedJobs = 0;

    @Column(name = "wallet_balance", precision = 12, scale = 0)
    private BigDecimal walletBalance = BigDecimal.ZERO;

    @Column(name = "is_online")
    private Boolean isOnline = false;
}
