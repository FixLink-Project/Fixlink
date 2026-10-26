package com.fixlink.adapter.out.persistence.repository;

import com.fixlink.adapter.out.persistence.entity.PriceQuoteJpaEntity;
import com.fixlink.domain.model.PriceQuoteStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataPriceQuoteRepository extends JpaRepository<PriceQuoteJpaEntity, Long> {

    List<PriceQuoteJpaEntity> findByRequestIdAndDeletedAtIsNull(Long requestId);

    Optional<PriceQuoteJpaEntity> findByRequestIdAndTechnicianIdAndDeletedAtIsNull(
            Long requestId, Long technicianId);

    List<PriceQuoteJpaEntity> findByRequestIdAndStatusAndDeletedAtIsNull(
            Long requestId, PriceQuoteStatus status);
}
