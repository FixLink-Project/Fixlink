package com.fixlink.adapter.out.persistence.entity;

import com.fixlink.domain.model.RequestStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "repair_requests")
@Getter
@Setter
public class RepairRequestJpaEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_code", nullable = false, unique = true, length = 50)
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
    @Column(name = "status", nullable = false, length = 30)
    private RequestStatus status = RequestStatus.PENDING;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "address", nullable = false)
    private String address;

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "requested_time", nullable = false)
    private LocalDateTime requestedTime;

    @Column(name = "agreed_price")
    private BigDecimal agreedPrice = BigDecimal.ZERO;

    @Column(name = "deposit_amount")
    private BigDecimal depositAmount = BigDecimal.ZERO;

    @Column(name = "budget_ref")
    private BigDecimal budgetRef = BigDecimal.ZERO;

    @Column(name = "bidding_deadline")
    private LocalDateTime biddingDeadline;

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;

    @Column(name = "selected_quotation_id")
    private Long selectedQuotationId;

    @Version
    @Column(name = "version")
    private Long version = 0L;
}
