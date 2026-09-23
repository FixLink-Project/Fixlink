package com.fixlink.adapter.out.persistence.repository;

import com.fixlink.adapter.out.persistence.entity.QuotationJpaEntity;
import com.fixlink.domain.model.QuotationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataQuotationRepository extends JpaRepository<QuotationJpaEntity, Long> {

    long countByRequestId(Long requestId);

    boolean existsByRequestIdAndTechnicianId(Long requestId, Long technicianId);

    List<QuotationJpaEntity> findByRequestIdOrderByCreatedAtDesc(Long requestId);

    Optional<QuotationJpaEntity> findByRequestIdAndTechnicianId(Long requestId, Long technicianId);

    List<QuotationJpaEntity> findByRequestIdAndStatus(Long requestId, QuotationStatus status);

    List<QuotationJpaEntity> findByTechnicianIdOrderByCreatedAtDesc(Long technicianId);
}
