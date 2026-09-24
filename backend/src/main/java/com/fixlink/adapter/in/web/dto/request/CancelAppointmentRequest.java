package com.fixlink.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Yêu cầu hủy lịch hẹn (Jira RC-48: Appointment Status Lifecycle).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelAppointmentRequest {

    @NotBlank(message = "Lý do hủy cuộc hẹn không được để trống")
    private String cancelReason;
}
