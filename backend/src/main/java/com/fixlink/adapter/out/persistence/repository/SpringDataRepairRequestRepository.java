package com.fixlink.adapter.out.persistence.repository;

import com.fixlink.adapter.out.persistence.entity.RepairRequestJpaEntity;
import com.fixlink.domain.model.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataRepairRequestRepository extends JpaRepository<RepairRequestJpaEntity, Long>,
        JpaSpecificationExecutor<RepairRequestJpaEntity> {
    /**
     * Khóa ghi bi quan (SELECT ... FOR UPDATE) trên đúng một yêu cầu.
     *
     * <p>Dùng cho luồng thợ "Nhận việc": hai thợ bấm cùng lúc thì DB xếp hàng,
     * thợ thứ hai chỉ đọc được bản ghi sau khi thợ thứ nhất đã commit, nên nhìn
     * thấy status đã là ASSIGNED và bị từ chối bằng lỗi JOB_ALREADY_TAKEN.
     * Không có bước khách xác nhận làm "chỗ đệm" nữa nên chặn ở đây là bắt buộc.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RepairRequestJpaEntity r WHERE r.id = :id")
    Optional<RepairRequestJpaEntity> findByIdForUpdate(@Param("id") Long id);

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
        WHERE r.status = com.fixlink.domain.model.RequestStatus.OPEN
          AND r.deletedAt IS NULL
          AND (r.applyDeadline IS NULL OR r.applyDeadline > CURRENT_TIMESTAMP)
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
          AND r.status = com.fixlink.domain.model.RequestStatus.OPEN
          AND r.categoryId IN :categoryIds
          AND r.areaId = :areaId
          AND (r.applyDeadline IS NULL OR r.applyDeadline > CURRENT_TIMESTAMP)
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

    /**
     * Các yêu cầu còn OPEN nhưng đã quá hạn nhận việc — dùng cho job quét định kỳ.
     */
    @Query("""
        SELECT r FROM RepairRequestJpaEntity r
        WHERE r.deletedAt IS NULL
          AND r.status = com.fixlink.domain.model.RequestStatus.OPEN
          AND r.applyDeadline IS NOT NULL
          AND r.applyDeadline <= :now
    """)
    List<RepairRequestJpaEntity> findExpiredOpenRequests(@Param("now") LocalDateTime now);
}
