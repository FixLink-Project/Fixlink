package com.fixlink.chatbot.application.service;

import com.fixlink.chatbot.application.port.in.SupportTicketUseCase;
import com.fixlink.chatbot.application.port.out.SupportTicketRepositoryPort;
import com.fixlink.chatbot.domain.model.SupportTicket;
import com.fixlink.chatbot.domain.model.TicketPriority;
import com.fixlink.chatbot.domain.model.TicketStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SupportTicketService implements SupportTicketUseCase {

    private static final Logger LOG = LoggerFactory.getLogger(SupportTicketService.class);

    private final SupportTicketRepositoryPort ticketRepository;
    private final AtomicLong ticketSequence = new AtomicLong(1000);
    private boolean sequenceInitialized = false;

    public SupportTicketService(SupportTicketRepositoryPort ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    private synchronized void initSequenceIfNeeded() {
        if (!sequenceInitialized) {
            long count = ticketRepository.count();
            ticketSequence.set(1000 + count);
            sequenceInitialized = true;
        }
    }

    @Override
    @Transactional
    public synchronized SupportTicket createTicket(
            Long userId,
            String guestName,
            String guestContact,
            String title,
            String description,
            TicketPriority priority,
            String assignee
    ) {
        initSequenceIfNeeded();
        long seq = ticketSequence.incrementAndGet();
        String ticketCode = "TCK-" + seq;

        Instant now = Instant.now();
        SupportTicket ticket = SupportTicket.builder()
                .ticketCode(ticketCode)
                .userId(userId)
                .guestName(guestName != null ? guestName.trim() : null)
                .guestContact(guestContact != null ? guestContact.trim() : null)
                .title(title != null ? title.trim() : "Yêu cầu hỗ trợ")
                .description(description != null ? description.trim() : "")
                .priority(priority != null ? priority : TicketPriority.MEDIUM)
                .status(TicketStatus.OPEN)
                .assignee(assignee != null && !assignee.isBlank() ? assignee.trim() : "Chưa phân công")
                .createdAt(now)
                .updatedAt(now)
                .build();

        SupportTicket saved = ticketRepository.save(ticket);
        LOG.info("Created support ticket {} for user {} / contact {}", ticketCode, userId, guestContact);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SupportTicket> getTicketByCode(
            String code,
            Long requesterId,
            boolean isAdmin,
            String guestContact
    ) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        String cleanCode = code.trim().toUpperCase();
        if (!cleanCode.startsWith("TCK-")) {
            cleanCode = "TCK-" + cleanCode;
        }

        Optional<SupportTicket> opt = ticketRepository.findByCode(cleanCode);
        if (opt.isEmpty()) {
            return Optional.empty();
        }

        SupportTicket ticket = opt.get();
        if (isAdmin) {
            return Optional.of(ticket);
        }

        if (requesterId != null && requesterId.equals(ticket.getUserId())) {
            return Optional.of(ticket);
        }

        if (guestContact != null && !guestContact.isBlank()
                && ticket.getGuestContact() != null
                && ticket.getGuestContact().equalsIgnoreCase(guestContact.trim())) {
            return Optional.of(ticket);
        }

        LOG.warn("Access denied for ticket {} (requesterId={}, contact={})", cleanCode, requesterId, guestContact);
        return Optional.empty();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportTicket> getMyTickets(Long userId) {
        if (userId == null) {
            return List.of();
        }
        return ticketRepository.findByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupportTicket> getAllTickets() {
        return ticketRepository.findAll();
    }
}
