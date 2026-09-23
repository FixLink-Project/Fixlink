package com.fixlink.adapter.out.persistence.repository;

import com.fixlink.adapter.out.persistence.entity.RepairRequestJpaEntity;
import com.fixlink.domain.model.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface SpringDataRepairRequestRepository extends JpaRepository<RepairRequestJpaEntity, Long> {

    Page<RepairRequestJpaEntity> findByCustomerIdAndStatusAndDeletedAtIsNull(Long customerId, RequestStatus status, Pageable pageable);

    Page<RepairRequestJpaEntity> findByCustomerIdAndDeletedAtIsNull(Long customerId, Pageable pageable);

    long countByCategoryId(Long categoryId);

    long countByAreaId(Long areaId);

    @Query("""
        SELECT r FROM RepairRequestJpaEntity r
        WHERE r.deletedAt IS NULL
          AND r.status = com.fixlink.domain.model.RequestStatus.BIDDING_OPEN
          AND (r.biddingDeadline IS NULL OR r.biddingDeadline > CURRENT_TIMESTAMP)
          AND r.customerId != :technicianId
          AND r.categoryId IN :categoryIds
          AND (r.areaId IS NULL OR r.areaId IN :areaIds)
    """)
    Page<RepairRequestJpaEntity> findMatchingForTechnician(
            @Param("categoryIds") Collection<Long> categoryIds,
            @Param("areaIds") Collection<Long> areaIds,
            @Param("technicianId") Long technicianId,
            Pageable pageable);
}
