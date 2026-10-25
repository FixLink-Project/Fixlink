package com.fixlink.adapter.out.persistence.repository;

import com.fixlink.adapter.out.persistence.entity.AdditionalCostJpaEntity;
import com.fixlink.domain.model.CostStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataAdditionalCostRepository extends JpaRepository<AdditionalCostJpaEntity, Long> {

    List<AdditionalCostJpaEntity> findByRequestIdAndDeletedAtIsNull(Long requestId);

    List<AdditionalCostJpaEntity> findByRequestIdAndStatusAndDeletedAtIsNull(
            Long requestId, CostStatus status);
}
