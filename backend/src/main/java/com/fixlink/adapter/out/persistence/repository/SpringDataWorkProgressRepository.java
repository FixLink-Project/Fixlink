package com.fixlink.adapter.out.persistence.repository;

import com.fixlink.adapter.out.persistence.entity.WorkProgressJpaEntity;
import com.fixlink.domain.model.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface SpringDataWorkProgressRepository extends JpaRepository<WorkProgressJpaEntity, Long> {

    List<WorkProgressJpaEntity> findByRequestIdOrderByCreatedAtAsc(Long requestId);

    List<WorkProgressJpaEntity> findByRequestIdInAndToStatus(List<Long> requestIds, RequestStatus toStatus);
    List<WorkProgressJpaEntity> findByRequestIdInAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            List<Long> requestIds, LocalDateTime from, LocalDateTime to);

    @Query("SELECT COUNT(w) FROM WorkProgressJpaEntity w WHERE w.createdAt >= :from AND w.createdAt < :to")
    long countCreatedBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(DISTINCT w.createdBy) FROM WorkProgressJpaEntity w WHERE w.createdBy IS NOT NULL AND w.createdAt >= :from AND w.createdAt < :to")
    long countActiveTechniciansBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    long countByToStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            RequestStatus status, LocalDateTime from, LocalDateTime to);
}
