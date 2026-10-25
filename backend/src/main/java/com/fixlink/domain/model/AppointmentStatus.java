package com.fixlink.domain.model;

/**
 * Trạng thái vòng đời cuộc hẹn (Jira RC-48: Appointment Status Lifecycle).
 *
 * <p>Enum domain thuần, không phụ thuộc Spring/JPA/Lombok.
 */
public enum AppointmentStatus {
    PENDING_APPROVAL("Chờ xác nhận", false),
    CONFIRMED("Đã xác nhận", false),
    REJECTED("Bị từ chối", true),
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

    public boolean canTransitionTo(AppointmentStatus nextStatus) {
        if (this.terminal) {
            return false;
        }
        if (nextStatus == null) {
            return false;
        }
        return switch (this) {
            case PENDING_APPROVAL -> nextStatus == CONFIRMED || nextStatus == REJECTED || nextStatus == CANCELLED;
            case CONFIRMED -> nextStatus == RESCHEDULED || nextStatus == COMPLETED || nextStatus == CANCELLED;
            case RESCHEDULED -> nextStatus == RESCHEDULED || nextStatus == COMPLETED || nextStatus == CANCELLED;
            default -> false;
        };
    }
}
