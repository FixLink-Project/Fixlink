package com.fixlink.adapter.in.web.dto.response;

import com.fixlink.domain.model.AppointmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Phản hồi chi tiết lịch hẹn (Jira RC-48: Appointment Status Lifecycle).
 * Chuẩn hóa theo database-erd.jpg.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentResponse {
    private Long id;
    private Long repairRequestId;
    private String requestCode;
    private String requestTitle;
    private String requestAddress;

    private Long customerId;
    private String customerName;
    private String customerPhone;

    private Long technicianId;
    private String technicianName;
    private String technicianPhone;

    private String appointmentType;
    private LocalDate scheduledDate;
    private LocalTime scheduledTime;
    private LocalDateTime actualStartAt;
    private LocalDateTime actualEndAt;
    private AppointmentStatus status;
    private String statusLabel;

    private String address;
    private String notes;
    private String cancelReason;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
