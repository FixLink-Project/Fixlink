package com.fixlink.chatbot.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SupportTicketJpaRepository extends JpaRepository<SupportTicketEntity, Long> {

    Optional<SupportTicketEntity> findByTicketCode(String ticketCode);

    List<SupportTicketEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<SupportTicketEntity> findAllByOrderByCreatedAtDesc();

    @Query("SELECT COUNT(t) FROM SupportTicketEntity t")
    long countAllTickets();
}
