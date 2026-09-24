package com.fixlink.adapter.out.persistence.repository;

import com.fixlink.adapter.out.persistence.entity.RepairRequestJpaEntity;
import com.fixlink.domain.model.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataRepairRequestRepository extends JpaRepository<RepairRequestJpaEntity, Long>,
        JpaSpecificationExecutor<RepairRequestJpaEntity> {
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
          AND (r.areaId IS NULL OR r.areaId IN :areaIds)
          AND r.customerId != :technicianId
          AND (
               :search IS NULL OR :search = ''
               OR LOWER(r.title) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(r.description) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(r.address) LIKE LOWER(CONCAT('%', :search, '%'))
          )
    """)
    Page<RepairRequestJpaEntity> findMatchingForTechnician(
            @Param("categoryIds") Collection<Long> categoryIds,
            @Param("areaIds") Collection<Long> areaIds,
            @Param("search") String search,
            @Param("technicianId") Long technicianId,
            Pageable pageable
    );

    @Query("""
        SELECT r FROM RepairRequestJpaEntity r
        WHERE r.deletedAt IS NULL
          AND r.status = com.fixlink.domain.model.RequestStatus.BIDDING_OPEN
          AND r.categoryId IN :categoryIds
          AND r.areaId = :areaId
          AND (r.biddingDeadline IS NULL OR r.biddingDeadline > CURRENT_TIMESTAMP)
          AND r.customerId != :technicianId
          AND (
               :search IS NULL OR :search = ''
               OR LOWER(r.title) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(r.description) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(r.address) LIKE LOWER(CONCAT('%', :search, '%'))
          )
    """)
    Page<RepairRequestJpaEntity> findMatchingForTechnicianByArea(
            @Param("categoryIds") Collection<Long> categoryIds,
            @Param("areaId") Long areaId,
            @Param("search") String search,
            @Param("technicianId") Long technicianId,
            Pageable pageable);
}
