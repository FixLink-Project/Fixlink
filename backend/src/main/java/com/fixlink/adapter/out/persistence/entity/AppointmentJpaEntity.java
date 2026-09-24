package com.fixlink.adapter.out.persistence.entity;

import com.fixlink.domain.model.AppointmentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Thực thể lịch hẹn (Jira RC-48: Appointment Status Lifecycle).
 * Chuẩn hóa 100% theo database-erd.jpg.
 */
@Entity
@Table(name = "appointments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentJpaEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "repair_request_id", nullable = false)
    private Long repairRequestId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "technician_id", nullable = false)
    private Long technicianId;

    @Column(name = "appointment_type", length = 30, nullable = false)
    @Builder.Default
    private String appointmentType = "REPAIR";

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(name = "scheduled_time", nullable = false)
    private LocalTime scheduledTime;

    @Column(name = "actual_start_at")
    private LocalDateTime actualStartAt;

    @Column(name = "actual_end_at")
    private LocalDateTime actualEndAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AppointmentStatus status = AppointmentStatus.CONFIRMED;

    @Column(name = "address")
    private String address;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;
}
