package com.fixlink.adapter.out.persistence.repository;

import com.fixlink.adapter.out.persistence.entity.RepairRequestReviewJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SpringDataRepairRequestReviewRepository extends JpaRepository<RepairRequestReviewJpaEntity, Long> {
    Optional<RepairRequestReviewJpaEntity> findByRequestIdAndDeletedAtIsNull(Long requestId);
    boolean existsByRequestIdAndDeletedAtIsNull(Long requestId);
    List<RepairRequestReviewJpaEntity> findByTechnicianIdAndDeletedAtIsNull(Long technicianId);

    @Query("SELECT AVG(r.rating) FROM RepairRequestReviewJpaEntity r, RepairRequestJpaEntity q " +
            "WHERE q.id = r.requestId AND r.deletedAt IS NULL AND r.createdAt >= :from AND r.createdAt < :to " +
            "AND (:areaId IS NULL OR q.areaId = :areaId)")
    Double averageRatingBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                                @Param("areaId") Long areaId);

    @Query("SELECT COUNT(r) FROM RepairRequestReviewJpaEntity r, RepairRequestJpaEntity q " +
            "WHERE q.id = r.requestId AND r.deletedAt IS NULL AND r.createdAt >= :from AND r.createdAt < :to " +
            "AND (:areaId IS NULL OR q.areaId = :areaId)")
    long countBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                      @Param("areaId") Long areaId);

    @Query("SELECT COUNT(r) FROM RepairRequestReviewJpaEntity r, RepairRequestJpaEntity q " +
            "WHERE q.id = r.requestId AND r.deletedAt IS NULL AND r.rating = :rating " +
            "AND r.createdAt >= :from AND r.createdAt < :to " +
            "AND (:areaId IS NULL OR q.areaId = :areaId)")
    long countRatingBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                            @Param("areaId") Long areaId, @Param("rating") int rating);
}
