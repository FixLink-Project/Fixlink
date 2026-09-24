package com.fixlink.adapter.out.persistence.repository;

import com.fixlink.adapter.out.persistence.entity.AuditLogJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataAuditLogRepository extends JpaRepository<AuditLogJpaEntity, Long> {
    List<AuditLogJpaEntity> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<AuditLogJpaEntity> findByEntityNameAndEntityIdOrderByCreatedAtDesc(String entityName, String entityId);
}
