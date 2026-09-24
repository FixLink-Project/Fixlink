package com.fixlink.adapter.out.persistence.entity;

import com.fixlink.domain.model.QuotationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "quotations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"request_id", "technician_id"}))
@Getter
@Setter
public class QuotationJpaEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false)
    private Long requestId;

    @Column(name = "technician_id", nullable = false)
    private Long technicianId;

    @Column(name = "solution", nullable = false, columnDefinition = "TEXT")
    private String solution;

    @Column(name = "price_labor_vnd", nullable = false)
    private BigDecimal priceLaborVnd = BigDecimal.ZERO;

    @Column(name = "price_materials_vnd", nullable = false)
    private BigDecimal priceMaterialsVnd = BigDecimal.ZERO;

    @Column(name = "inspection_time")
    private LocalDateTime inspectionTime;

    @Column(name = "estimated_finish")
    private LocalDateTime estimatedFinish;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private QuotationStatus status = QuotationStatus.PENDING;
}
