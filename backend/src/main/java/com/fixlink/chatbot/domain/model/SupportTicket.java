package com.fixlink.chatbot.domain.model;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportTicket {
    private Long id;
    private String ticketCode;
    private Long userId;
    private String guestName;
    private String guestContact;
    private String title;
    private String description;
    private TicketPriority priority;
    private TicketStatus status;
    private String assignee;
    private Instant createdAt;
    private Instant updatedAt;
}
