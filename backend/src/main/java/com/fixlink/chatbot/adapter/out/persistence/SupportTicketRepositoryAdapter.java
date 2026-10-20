package com.fixlink.chatbot.adapter.out.persistence;

import com.fixlink.chatbot.application.port.out.SupportTicketRepositoryPort;
import com.fixlink.chatbot.domain.model.SupportTicket;
import com.fixlink.chatbot.domain.model.TicketPriority;
import com.fixlink.chatbot.domain.model.TicketStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class SupportTicketRepositoryAdapter implements SupportTicketRepositoryPort {

    private final SupportTicketJpaRepository jpaRepository;

    public SupportTicketRepositoryAdapter(SupportTicketJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public SupportTicket save(SupportTicket ticket) {
        SupportTicketEntity entity = toEntity(ticket);
        SupportTicketEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<SupportTicket> findByCode(String ticketCode) {
        return jpaRepository.findByTicketCode(ticketCode).map(this::toDomain);
    }

    @Override
    public List<SupportTicket> findByUserId(Long userId) {
        return jpaRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<SupportTicket> findAll() {
        return jpaRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long count() {
        return jpaRepository.countAllTickets();
    }

    private SupportTicketEntity toEntity(SupportTicket domain) {
        return SupportTicketEntity.builder()
                .id(domain.getId())
                .ticketCode(domain.getTicketCode())
                .userId(domain.getUserId())
                .guestName(domain.getGuestName())
                .guestContact(domain.getGuestContact())
                .title(domain.getTitle())
                .description(domain.getDescription())
                .priority(domain.getPriority() != null ? domain.getPriority().getValue() : "Medium")
                .status(domain.getStatus() != null ? domain.getStatus().getValue() : "Open")
                .assignee(domain.getAssignee() != null ? domain.getAssignee() : "Chưa phân công")
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    private SupportTicket toDomain(SupportTicketEntity entity) {
        return SupportTicket.builder()
                .id(entity.getId())
                .ticketCode(entity.getTicketCode())
                .userId(entity.getUserId())
                .guestName(entity.getGuestName())
                .guestContact(entity.getGuestContact())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .priority(TicketPriority.fromString(entity.getPriority()))
                .status(TicketStatus.fromString(entity.getStatus()))
                .assignee(entity.getAssignee())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
