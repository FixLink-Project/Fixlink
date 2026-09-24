package com.fixlink.adapter.out.persistence.entity;

import com.fixlink.domain.model.VerificationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "technician_verifications")
@Getter
@Setter
public class TechnicianVerificationJpaEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "technician_id", nullable = false)
    private Long technicianId;

    @Column(name = "admin_id")
    private Long adminId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private VerificationStatus status = VerificationStatus.PENDING;

    @Column(name = "citizen_id", nullable = false, length = 20)
    private String citizenId;

    @Column(name = "id_card_front_url", nullable = false)
    private String idCardFrontUrl;

    @Column(name = "id_card_back_url", nullable = false)
    private String idCardBackUrl;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
}
