package com.fixlink.adapter.out.persistence.entity;

import com.fixlink.domain.model.PriceQuoteStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "price_quotes",
       uniqueConstraints = @UniqueConstraint(columnNames = {"request_id", "technician_id"}))
public class PriceQuoteJpaEntity extends BaseJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false)
    private Long requestId;

    @Column(name = "technician_id", nullable = false)
    private Long technicianId;

    @Column(name = "quoted_price", precision = 15, scale = 0, nullable = false)
    private BigDecimal quotedPrice;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private PriceQuoteStatus status = PriceQuoteStatus.PENDING;
}
