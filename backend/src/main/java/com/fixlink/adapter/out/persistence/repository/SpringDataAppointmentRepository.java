package com.fixlink.adapter.out.persistence.repository;

import com.fixlink.adapter.out.persistence.entity.AppointmentJpaEntity;
import com.fixlink.domain.model.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository truy vấn lịch hẹn (Jira RC-48: Appointment Status Lifecycle).
 */
@Repository
public interface SpringDataAppointmentRepository extends JpaRepository<AppointmentJpaEntity, Long> {

    List<AppointmentJpaEntity> findByRepairRequestIdOrderByScheduledDateAscScheduledTimeAsc(Long repairRequestId);

    List<AppointmentJpaEntity> findByCustomerIdOrderByScheduledDateDescScheduledTimeDesc(Long customerId);

    List<AppointmentJpaEntity> findByTechnicianIdOrderByScheduledDateDescScheduledTimeDesc(Long technicianId);

    List<AppointmentJpaEntity> findByRepairRequestIdInAndDeletedAtIsNull(List<Long> repairRequestIds);

    List<AppointmentJpaEntity> findByRepairRequestIdAndStatus(Long repairRequestId, AppointmentStatus status);

    @Query("SELECT COUNT(a) FROM AppointmentJpaEntity a WHERE a.deletedAt IS NULL AND a.createdAt >= :from AND a.createdAt < :to")
    long countCreatedBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(DISTINCT a.customerId) FROM AppointmentJpaEntity a WHERE a.deletedAt IS NULL AND a.createdAt >= :from AND a.createdAt < :to")
    long countActiveCustomersBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT COUNT(DISTINCT a.technicianId) FROM AppointmentJpaEntity a WHERE a.deletedAt IS NULL AND a.createdAt >= :from AND a.createdAt < :to")
    long countActiveTechniciansBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
