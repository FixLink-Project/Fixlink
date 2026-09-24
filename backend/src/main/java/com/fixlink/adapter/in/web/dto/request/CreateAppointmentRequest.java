package com.fixlink.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Yêu cầu tạo lịch hẹn (Jira RC-48: Appointment Status Lifecycle).
 * Chuẩn hóa theo database-erd.jpg.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAppointmentRequest {

    @NotNull(message = "Mã yêu cầu sửa chữa không được để trống")
    private Long repairRequestId;

    private Long technicianId;

    @Builder.Default
    private String appointmentType = "REPAIR";

    @NotNull(message = "Ngày hẹn không được để trống")
    private LocalDate scheduledDate;

    @NotNull(message = "Giờ hẹn không được để trống")
    private LocalTime scheduledTime;

    private String address;

    private String notes;
}
