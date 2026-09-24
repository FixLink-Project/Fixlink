package com.fixlink.adapter.out.persistence.repository;

import com.fixlink.adapter.out.persistence.entity.AppointmentJpaEntity;
import com.fixlink.domain.model.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository truy vấn lịch hẹn (Jira RC-48: Appointment Status Lifecycle).
 */
@Repository
public interface SpringDataAppointmentRepository extends JpaRepository<AppointmentJpaEntity, Long> {

    List<AppointmentJpaEntity> findByRepairRequestIdOrderByScheduledDateAscScheduledTimeAsc(Long repairRequestId);

    List<AppointmentJpaEntity> findByCustomerIdOrderByScheduledDateDescScheduledTimeDesc(Long customerId);

    List<AppointmentJpaEntity> findByTechnicianIdOrderByScheduledDateDescScheduledTimeDesc(Long technicianId);

    List<AppointmentJpaEntity> findByRepairRequestIdAndStatus(Long repairRequestId, AppointmentStatus status);
}
