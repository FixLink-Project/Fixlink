package com.fixlink.adapter.out.persistence.entity;

import com.fixlink.domain.model.RequestStatus;
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
@Table(name = "repair_requests")
public class RepairRequestJpaEntity extends BaseJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_code", length = 50, nullable = false, unique = true)
    private String requestCode;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "technician_id")
    private Long technicianId;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "service_id")
    private Long serviceId;

    @Column(name = "area_id")
    private Long areaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    @Builder.Default
    private RequestStatus status = RequestStatus.PENDING;

    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(name = "device_brand", length = 100)
    private String deviceBrand;

    @Column(name = "device_model", length = 100)
    private String deviceModel;

    @Column(name = "serial_number", length = 100)
    private String serialNumber;

    @Column(name = "address", length = 255, nullable = false)
    private String address;

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "requested_time", nullable = false)
    private LocalDateTime requestedTime;

    @Column(name = "agreed_price", precision = 12, scale = 0)
    @Builder.Default
    private BigDecimal agreedPrice = BigDecimal.ZERO;

    @Column(name = "deposit_amount", precision = 12, scale = 0)
    @Builder.Default
    private BigDecimal depositAmount = BigDecimal.ZERO;

    @Column(name = "budget_ref", precision = 12, scale = 0)
    @Builder.Default
    private BigDecimal budgetRef = BigDecimal.ZERO;

    @Column(name = "bidding_deadline")
    private LocalDateTime biddingDeadline;

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;

    @Column(name = "selected_quotation_id")
    private Long selectedQuotationId;

    @Version
    @Column(name = "version")
    @Builder.Default
    private Long version = 0L;
}
