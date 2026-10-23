package com.fixlink.adapter.out.persistence.repository;

import com.fixlink.adapter.out.persistence.entity.QuotationJpaEntity;
import com.fixlink.domain.model.QuotationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface SpringDataQuotationRepository extends JpaRepository<QuotationJpaEntity, Long> {

    long countByRequestId(Long requestId);

    boolean existsByRequestIdAndTechnicianId(Long requestId, Long technicianId);

    List<QuotationJpaEntity> findByRequestIdOrderByCreatedAtDesc(Long requestId);

    Optional<QuotationJpaEntity> findByRequestIdAndTechnicianId(Long requestId, Long technicianId);

    List<QuotationJpaEntity> findByRequestIdAndStatus(Long requestId, QuotationStatus status);

    List<QuotationJpaEntity> findByTechnicianIdOrderByCreatedAtDesc(Long technicianId);

    List<QuotationJpaEntity> findByRequestIdInAndDeletedAtIsNull(List<Long> requestIds);

    @Query("SELECT COUNT(q) FROM QuotationJpaEntity q WHERE q.deletedAt IS NULL AND q.createdAt >= :from AND q.createdAt < :to")
    long countCreatedBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(DISTINCT q.technicianId) FROM QuotationJpaEntity q WHERE q.deletedAt IS NULL AND q.createdAt >= :from AND q.createdAt < :to")
    long countActiveTechniciansBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(DISTINCT r.customerId) FROM QuotationJpaEntity q JOIN RepairRequestJpaEntity r ON r.id = q.requestId WHERE q.deletedAt IS NULL AND r.deletedAt IS NULL AND q.createdAt >= :from AND q.createdAt < :to")
    long countCustomersReceivingQuotesBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
