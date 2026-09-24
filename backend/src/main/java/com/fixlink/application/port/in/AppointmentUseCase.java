package com.fixlink.application.port.in;

import com.fixlink.adapter.in.web.dto.response.AppointmentResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * UseCase quản lý vòng đời cuộc hẹn (Jira RC-48: Appointment Status Lifecycle).
 * Chuẩn hóa 100% theo database-erd.jpg.
 */
public interface AppointmentUseCase {

    AppointmentResponse createAppointment(CreateAppointmentCommand command, Long actorId, String role);

    AppointmentResponse rescheduleAppointment(Long appointmentId, RescheduleAppointmentCommand command, Long actorId, String role);

    AppointmentResponse completeAppointment(Long appointmentId, String note, Long actorId, String role);

    AppointmentResponse cancelAppointment(Long appointmentId, String reason, Long actorId, String role);

    AppointmentResponse getAppointmentDetail(Long appointmentId, Long actorId, String role);

    List<AppointmentResponse> getAppointmentsForRequest(Long requestId, Long actorId, String role);

    List<AppointmentResponse> getMyAppointments(Long actorId, String role);

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class CreateAppointmentCommand {
        private Long repairRequestId;
        private Long technicianId;
        private String appointmentType;
        private LocalDate scheduledDate;
        private LocalTime scheduledTime;
        private String address;
        private String notes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class RescheduleAppointmentCommand {
        private LocalDate scheduledDate;
        private LocalTime scheduledTime;
        private String notes;
    }
}
