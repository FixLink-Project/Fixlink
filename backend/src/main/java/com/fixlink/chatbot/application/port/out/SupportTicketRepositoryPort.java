package com.fixlink.chatbot.application.port.out;

import com.fixlink.chatbot.domain.model.SupportTicket;
import java.util.List;
import java.util.Optional;

public interface SupportTicketRepositoryPort {
    SupportTicket save(SupportTicket ticket);

    Optional<SupportTicket> findByCode(String ticketCode);

    List<SupportTicket> findByUserId(Long userId);

    List<SupportTicket> findAll();

    long count();
}
