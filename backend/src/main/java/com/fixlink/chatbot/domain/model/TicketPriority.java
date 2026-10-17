package com.fixlink.chatbot.domain.model;

public enum TicketPriority {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
    CRITICAL("Critical");

    private final String value;

    TicketPriority(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static TicketPriority fromString(String text) {
        if (text == null || text.isBlank()) {
            return MEDIUM;
        }
        for (TicketPriority p : values()) {
            if (p.name().equalsIgnoreCase(text) || p.value.equalsIgnoreCase(text)) {
                return p;
            }
        }
        return MEDIUM;
    }
}
