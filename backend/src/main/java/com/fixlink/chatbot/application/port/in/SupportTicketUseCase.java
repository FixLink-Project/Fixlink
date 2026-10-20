package com.fixlink.chatbot.application.port.in;

import com.fixlink.chatbot.domain.model.SupportTicket;
import com.fixlink.chatbot.domain.model.TicketPriority;
import java.util.List;
import java.util.Optional;

public interface SupportTicketUseCase {

    SupportTicket createTicket(
            Long userId,
            String guestName,
            String guestContact,
            String title,
            String description,
            TicketPriority priority,
            String assignee
    );

    Optional<SupportTicket> getTicketByCode(
            String code,
            Long requesterId,
            boolean isAdmin,
            String guestContact
    );

    List<SupportTicket> getMyTickets(Long userId);

    List<SupportTicket> getAllTickets();
}
