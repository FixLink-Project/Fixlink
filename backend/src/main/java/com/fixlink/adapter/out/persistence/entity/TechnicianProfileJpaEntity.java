package com.fixlink.adapter.out.persistence.entity;

import com.fixlink.domain.model.VerificationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.Persistable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "technician_profiles")
public class TechnicianProfileJpaEntity extends BaseJpaEntity implements Persistable<Long> {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private UserJpaEntity user;

    @Column(name = "full_name", length = 100, nullable = false)
    private String fullName;

    @Column(name = "phone", length = 20, nullable = false, unique = true)
    private String phone;

    @Column(name = "email", length = 100, nullable = false, unique = true)
    private String email;

    @Column(name = "citizen_id", length = 20, nullable = false, unique = true)
    private String citizenId;

    @Column(name = "id_card_front_url", length = 500)
    private String idCardFrontUrl;

    @Column(name = "id_card_back_url", length = 500)
    private String idCardBackUrl;

    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "years_experience")
    @Builder.Default
    private Integer yearsExperience = 0;

    @Column(name = "avg_rating", precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal avgRating = BigDecimal.ZERO;

    @Column(name = "completed_jobs")
    @Builder.Default
    private Integer completedJobs = 0;

    @Column(name = "wallet_balance", precision = 12, scale = 0)
    @Builder.Default
    private BigDecimal walletBalance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", length = 20, nullable = false)
    @Builder.Default
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "verified_by", length = 50)
    private String verifiedBy;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "is_online")
    @Builder.Default
    private Boolean isOnline = false;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "technician_categories", joinColumns = @JoinColumn(name = "technician_id"))
    @Column(name = "category_id")
    @Builder.Default
    private Set<Long> categoryIds = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "technician_areas", joinColumns = @JoinColumn(name = "technician_id"))
    @Column(name = "area_id")
    @Builder.Default
    private Set<Long> areaIds = new HashSet<>();

    @Transient
    @Builder.Default
    private boolean isNewEntity = true;

    @Override
    public Long getId() {
        return userId;
    }

    @Override
    public boolean isNew() {
        return isNewEntity;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNewEntity = false;
    }

    public void setUser(UserJpaEntity user) {
        this.user = user;
        if (user != null && user.getId() != null) {
            this.userId = user.getId();
        }
    }
}
