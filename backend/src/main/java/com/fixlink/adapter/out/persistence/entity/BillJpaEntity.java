package com.fixlink.adapter.out.persistence.entity;

import com.fixlink.domain.model.BillType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "bills")
public class BillJpaEntity extends BaseJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bill_number", length = 30, nullable = false, unique = true)
    private String billNumber;

    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "bill_type", length = 20, nullable = false)
    private BillType billType;

    @Column(name = "customer_name")
    private String customerName;
    @Column(name = "customer_phone", length = 20)
    private String customerPhone;
    @Column(name = "customer_address", columnDefinition = "TEXT")
    private String customerAddress;

    @Column(name = "technician_name")
    private String technicianName;
    @Column(name = "technician_phone", length = 20)
    private String technicianPhone;

    @Column(name = "service_category", length = 100)
    private String serviceCategory;
    @Column(name = "service_description", columnDefinition = "TEXT")
    private String serviceDescription;

    @Column(name = "request_id", nullable = false)
    private Long requestId;

    @Column(name = "quoted_price", precision = 15, scale = 0)
    @Builder.Default
    private BigDecimal quotedPrice = BigDecimal.ZERO;

    @Column(name = "deposit_amount", precision = 15, scale = 0)
    @Builder.Default
    private BigDecimal depositAmount = BigDecimal.ZERO;

    @Column(name = "additional_cost", precision = 15, scale = 0)
    @Builder.Default
    private BigDecimal additionalCost = BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 15, scale = 0, nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "payment_method", length = 30)
    @Builder.Default
    private String paymentMethod = "VIETQR";
}
