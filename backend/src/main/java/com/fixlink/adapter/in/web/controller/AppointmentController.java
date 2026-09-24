package com.fixlink.adapter.in.web.controller;

import com.fixlink.adapter.in.web.dto.request.CancelAppointmentRequest;
import com.fixlink.adapter.in.web.dto.request.CreateAppointmentRequest;
import com.fixlink.adapter.in.web.dto.request.RescheduleAppointmentRequest;
import com.fixlink.adapter.in.web.dto.response.ApiResponse;
import com.fixlink.adapter.in.web.dto.response.AppointmentResponse;
import com.fixlink.application.port.in.AppointmentUseCase;
import com.fixlink.infrastructure.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller quản lý vòng đời cuộc hẹn (Jira RC-48: Appointment Status Lifecycle).
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Appointments", description = "Quản lý vòng đời cuộc hẹn (RC-48)")
@SecurityRequirement(name = "bearerAuth")
public class AppointmentController {

    private final AppointmentUseCase appointmentUseCase;

    @PostMapping("/api/v1/appointments")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Tạo lịch hẹn mới (CONFIRMED)")
    public ResponseEntity<ApiResponse<AppointmentResponse>> create(
            @Valid @RequestBody CreateAppointmentRequest req,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        var cmd = AppointmentUseCase.CreateAppointmentCommand.builder()
                .repairRequestId(req.getRepairRequestId())
                .technicianId(req.getTechnicianId())
                .appointmentType(req.getAppointmentType())
                .scheduledDate(req.getScheduledDate())
                .scheduledTime(req.getScheduledTime())
                .address(req.getAddress())
                .notes(req.getNotes())
                .build();

        AppointmentResponse result = appointmentUseCase.createAppointment(cmd, user.getId(), user.getRole().name());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Tạo lịch hẹn thành công", result));
    }

    @GetMapping("/api/v1/appointments/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Xem chi tiết một lịch hẹn")
    public ResponseEntity<ApiResponse<AppointmentResponse>> getDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        AppointmentResponse result = appointmentUseCase.getAppointmentDetail(id, user.getId(), user.getRole().name());
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin lịch hẹn thành công", result));
    }

    @GetMapping("/api/v1/repair-requests/{requestId}/appointments")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lấy danh sách các lịch hẹn của một yêu cầu sửa chữa")
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getAppointmentsForRequest(
            @PathVariable Long requestId,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        List<AppointmentResponse> result = appointmentUseCase.getAppointmentsForRequest(requestId, user.getId(), user.getRole().name());
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách lịch hẹn thành công", result));
    }

    @GetMapping("/api/v1/appointments/my")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lấy danh sách tất cả các lịch hẹn của tôi")
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getMyAppointments(
            @AuthenticationPrincipal UserPrincipal user
    ) {
        List<AppointmentResponse> result = appointmentUseCase.getMyAppointments(user.getId(), user.getRole().name());
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách lịch hẹn của tôi thành công", result));
    }

    @PatchMapping("/api/v1/appointments/{id}/reschedule")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Đổi lịch hẹn sang ngày giờ mới (RESCHEDULED)")
    public ResponseEntity<ApiResponse<AppointmentResponse>> reschedule(
            @PathVariable Long id,
            @Valid @RequestBody RescheduleAppointmentRequest req,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        var cmd = AppointmentUseCase.RescheduleAppointmentCommand.builder()
                .scheduledDate(req.getScheduledDate())
                .scheduledTime(req.getScheduledTime())
                .notes(req.getNotes())
                .build();

        AppointmentResponse result = appointmentUseCase.rescheduleAppointment(id, cmd, user.getId(), user.getRole().name());
        return ResponseEntity.ok(ApiResponse.success("Dời lịch hẹn thành công", result));
    }

    @PatchMapping("/api/v1/appointments/{id}/complete")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Xác nhận hoàn thành lịch hẹn (COMPLETED)")
    public ResponseEntity<ApiResponse<AppointmentResponse>> complete(
            @PathVariable Long id,
            @RequestParam(required = false) String note,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        AppointmentResponse result = appointmentUseCase.completeAppointment(id, note, user.getId(), user.getRole().name());
        return ResponseEntity.ok(ApiResponse.success("Xác nhận hoàn thành lịch hẹn thành công", result));
    }

    @PatchMapping("/api/v1/appointments/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Hủy lịch hẹn kèm lý do bắt buộc (CANCELLED)")
    public ResponseEntity<ApiResponse<AppointmentResponse>> cancel(
            @PathVariable Long id,
            @Valid @RequestBody CancelAppointmentRequest req,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        AppointmentResponse result = appointmentUseCase.cancelAppointment(id, req.getCancelReason(), user.getId(), user.getRole().name());
        return ResponseEntity.ok(ApiResponse.success("Hủy lịch hẹn thành công", result));
    }
}
