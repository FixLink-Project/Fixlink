package com.fixlink.adapter.out.persistence.repository;

import com.fixlink.adapter.out.persistence.entity.WorkProgressJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpringDataWorkProgressRepository extends JpaRepository<WorkProgressJpaEntity, Long> {
    List<WorkProgressJpaEntity> findByRequestIdOrderByCreatedAtAsc(Long requestId);
}
