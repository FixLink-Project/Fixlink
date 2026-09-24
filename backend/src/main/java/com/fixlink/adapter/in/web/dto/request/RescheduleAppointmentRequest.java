package com.fixlink.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Yêu cầu đổi ngày giờ hẹn (Jira RC-48: Appointment Status Lifecycle).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RescheduleAppointmentRequest {

    @NotNull(message = "Ngày hẹn mới không được để trống")
    private LocalDate scheduledDate;

    @NotNull(message = "Giờ hẹn mới không được để trống")
    private LocalTime scheduledTime;

    private String notes;
}
