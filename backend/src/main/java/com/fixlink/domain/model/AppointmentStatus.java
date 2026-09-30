package com.fixlink.domain.model;

/**
 * Trạng thái vòng đời cuộc hẹn (Jira RC-48: Appointment Status Lifecycle).
 *
 * <p>Enum domain thuần, không phụ thuộc Spring/JPA/Lombok.
 */
public enum AppointmentStatus {
    CONFIRMED("Đã xác nhận", false),
    RESCHEDULED("Đã dời lịch", false),
    COMPLETED("Đã hoàn thành", true),
    CANCELLED("Đã hủy", true);

    private final String label;
    private final boolean terminal;

    AppointmentStatus(String label, boolean terminal) {
        this.label = label;
        this.terminal = terminal;
    }

    public String getLabel() {
        return label;
    }

    public boolean isTerminal() {
        return terminal;
    }

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
