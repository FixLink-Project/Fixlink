package com.fixlink.adapter.out.persistence.entity;

import com.fixlink.domain.model.QuotationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "quotations")
public class QuotationJpaEntity extends BaseJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false)
    private Long requestId;

    @Column(name = "technician_id", nullable = false)
    private Long technicianId;

    @Column(name = "solution", columnDefinition = "TEXT", nullable = false)
    private String solution;

    @Column(name = "price_labor_vnd", precision = 12, scale = 0, nullable = false)
    @Builder.Default
    private BigDecimal priceLaborVnd = BigDecimal.ZERO;

    @Column(name = "price_materials_vnd", precision = 12, scale = 0, nullable = false)
    @Builder.Default
    private BigDecimal priceMaterialsVnd = BigDecimal.ZERO;

    @Column(name = "inspection_time")
    private LocalDateTime inspectionTime;

    @Column(name = "estimated_finish")
    private LocalDateTime estimatedFinish;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private QuotationStatus status = QuotationStatus.PENDING;
}
