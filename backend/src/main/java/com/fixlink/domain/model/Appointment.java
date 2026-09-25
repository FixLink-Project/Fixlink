package com.fixlink.domain.model;

import com.fixlink.domain.exception.DomainException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Thực thể miền Cuộc hẹn sửa chữa / khảo sát với Kỹ thuật viên (Feature RC-47).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Appointment {

    private Long id;
    private Long repairRequestId;
    private Long customerId;
    private Long technicianId;
    private LocalDate scheduledDate;
    private LocalTime scheduledTime;
    private LocalDateTime appointmentDateTime;
    private AppointmentStatus status;
    private String address;
    private String notes;
    private String cancellationReason;

    private LocalDateTime createdAt;
    private Long createdBy;
    private LocalDateTime updatedAt;
    private Long updatedBy;
    private LocalDateTime deletedAt;
    private Long deletedBy;

    /**
     * Thẩm định tính hợp lệ của cuộc hẹn.
     */
    public void validate() {
        if (customerId == null) {
            throw new DomainException("INVALID_APPOINTMENT_DATA", "Mã khách hàng không được để trống", 400);
        }
        if (technicianId == null) {
            throw new DomainException("INVALID_APPOINTMENT_DATA", "Mã kỹ thuật viên không được để trống", 400);
        }
        if (customerId.equals(technicianId)) {
            throw new DomainException("SELF_APPOINTMENT_NOT_ALLOWED", "Khách hàng không thể tự đặt lịch hẹn với chính mình", 400);
        }
        if (scheduledDate == null || scheduledTime == null) {
            throw new DomainException("INVALID_APPOINTMENT_TIME", "Ngày và giờ hẹn không được để trống", 400);
        }
        if (appointmentDateTime == null) {
            appointmentDateTime = LocalDateTime.of(scheduledDate, scheduledTime);
        }
        if (appointmentDateTime.isBefore(LocalDateTime.now())) {
            throw new DomainException("INVALID_APPOINTMENT_TIME", "Thời gian hẹn phải ở trong tương lai", 400);
        }
        if (address == null || address.trim().isEmpty()) {
            throw new DomainException("INVALID_APPOINTMENT_DATA", "Địa chỉ cuộc hẹn không được để trống", 400);
        }
        if (notes == null || notes.trim().isEmpty()) {
            throw new DomainException("INVALID_APPOINTMENT_DATA", "Mô tả công việc hoặc ghi chú không được để trống", 400);
        }
        if (status == null) {
            status = AppointmentStatus.CONFIRMED;
        }
    }

    /**
     * Hủy cuộc hẹn kèm lý do hợp lệ.
     */
    public void cancel(Long requesterId, String reason, String requesterRole) {
        if (this.status == AppointmentStatus.CANCELLED) {
            throw new DomainException("APPOINTMENT_ALREADY_CANCELLED", "Lịch hẹn này đã bị hủy trước đó", 400);
        }
        if (this.status == AppointmentStatus.COMPLETED) {
            throw new DomainException("CANNOT_CANCEL_COMPLETED_APPOINTMENT", "Không thể hủy lịch hẹn đã hoàn thành", 400);
        }
        boolean isOwner = requesterId != null && (requesterId.equals(customerId) || requesterId.equals(technicianId));
        boolean isAdmin = requesterRole != null && (requesterRole.contains("ADMIN") || requesterRole.contains("ROLE_ADMIN"));
        if (!isOwner && !isAdmin) {
            throw new DomainException("ACCESS_DENIED", "Bạn không có quyền hủy lịch hẹn này", 403);
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new DomainException("CANCELLATION_REASON_REQUIRED", "Vui lòng cung cấp lý do hủy lịch hẹn", 400);
        }

        this.status = AppointmentStatus.CANCELLED;
        this.cancellationReason = reason.trim();
        this.updatedAt = LocalDateTime.now();
        this.updatedBy = requesterId;
    }

    /**
     * Xác nhận cuộc hẹn.
     */
    public void confirm() {
        if (this.status == AppointmentStatus.CANCELLED) {
            throw new DomainException("INVALID_STATUS_TRANSITION", "Không thể xác nhận lịch hẹn đã hủy", 400);
        }
        this.status = AppointmentStatus.CONFIRMED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Đánh dấu hoàn thành cuộc hẹn.
     */
    public void complete() {
        if (this.status == AppointmentStatus.CANCELLED) {
            throw new DomainException("INVALID_STATUS_TRANSITION", "Không thể hoàn thành lịch hẹn đã hủy", 400);
        }
        this.status = AppointmentStatus.COMPLETED;
        this.updatedAt = LocalDateTime.now();
    }
}
