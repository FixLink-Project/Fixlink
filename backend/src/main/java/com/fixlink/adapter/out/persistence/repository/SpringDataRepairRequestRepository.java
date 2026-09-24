package com.fixlink.adapter.out.persistence.repository;

import com.fixlink.adapter.out.persistence.entity.RepairRequestJpaEntity;
import com.fixlink.domain.model.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataRepairRequestRepository extends JpaRepository<RepairRequestJpaEntity, Long> {
    Optional<RepairRequestJpaEntity> findByRequestCode(String requestCode);
    List<RepairRequestJpaEntity> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    List<RepairRequestJpaEntity> findByTechnicianIdOrderByCreatedAtDesc(Long technicianId);
    List<RepairRequestJpaEntity> findByStatus(RequestStatus status);
    long countByCategoryId(Long categoryId);

    Page<RepairRequestJpaEntity> findByCustomerIdAndDeletedAtIsNull(Long customerId, Pageable pageable);

    Page<RepairRequestJpaEntity> findByCustomerIdAndStatusAndDeletedAtIsNull(
            Long customerId, RequestStatus status, Pageable pageable);

    @Query("""
        SELECT r FROM RepairRequestJpaEntity r
        WHERE r.status = 'BIDDING_OPEN'
          AND r.deletedAt IS NULL
          AND (r.biddingDeadline IS NULL OR r.biddingDeadline > CURRENT_TIMESTAMP)
          AND r.categoryId IN :categoryIds
          AND r.areaId IN :areaIds
          AND NOT EXISTS (
              SELECT 1 FROM QuotationJpaEntity q
              WHERE q.requestId = r.id AND q.technicianId = :technicianId
          )
    """)
    Page<RepairRequestJpaEntity> findMatchingForTechnician(
            @Param("categoryIds") java.util.Set<Long> categoryIds,
            @Param("areaIds") java.util.Set<Long> areaIds,
            @Param("technicianId") Long technicianId,
            Pageable pageable);
}
