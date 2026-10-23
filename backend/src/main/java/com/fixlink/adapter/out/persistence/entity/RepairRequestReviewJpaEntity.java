package com.fixlink.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "repair_request_reviews")
public class RepairRequestReviewJpaEntity extends BaseJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false, unique = true)
    private Long requestId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "technician_id", nullable = false)
    private Long technicianId;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "comment", nullable = false, columnDefinition = "TEXT")
    private String comment;
}
