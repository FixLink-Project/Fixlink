package com.fixlink.chatbot.domain.model;

public enum TicketStatus {
    OPEN("Open"),
    IN_PROGRESS("In_Progress"),
    RESOLVED("Resolved"),
    CLOSED("Closed");

    private final String value;

    TicketStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static TicketStatus fromString(String text) {
        if (text == null || text.isBlank()) {
            return OPEN;
        }
        for (TicketStatus s : values()) {
            if (s.name().equalsIgnoreCase(text) || s.value.equalsIgnoreCase(text)) {
                return s;
            }
        }
        return OPEN;
    }
}
