package com.fixlink.domain.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Trạng thái vòng đời cuộc hẹn (Jira RC-48: Appointment Status Lifecycle).
 */
@Getter
@RequiredArgsConstructor
public enum AppointmentStatus {
    CONFIRMED("Đã xác nhận", false),
    RESCHEDULED("Đã dời lịch", false),
    COMPLETED("Đã hoàn thành", true),
    CANCELLED("Đã hủy", true);

    private final String label;
    private final boolean terminal;

    /**
     * Kiểm tra tính hợp lệ của việc chuyển trạng thái trong máy trạng thái (State Machine).
     */
    public boolean canTransitionTo(AppointmentStatus nextStatus) {
        if (this.terminal) {
            return false;
        }
        if (nextStatus == null) {
            return false;
        }
        return switch (this) {
            case CONFIRMED -> nextStatus == RESCHEDULED || nextStatus == COMPLETED || nextStatus == CANCELLED;
            case RESCHEDULED -> nextStatus == RESCHEDULED || nextStatus == COMPLETED || nextStatus == CANCELLED;
            default -> false;
        };
    }
}
