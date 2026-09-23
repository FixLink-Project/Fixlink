package com.fixlink.adapter.out.persistence.repository;

import com.fixlink.adapter.out.persistence.entity.TechnicianVerificationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataTechnicianVerificationRepository extends JpaRepository<TechnicianVerificationJpaEntity, Long> {
    List<TechnicianVerificationJpaEntity> findByTechnicianIdOrderByCreatedAtDesc(Long technicianId);
}
