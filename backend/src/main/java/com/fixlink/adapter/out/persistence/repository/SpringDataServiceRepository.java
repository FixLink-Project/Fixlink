package com.fixlink.adapter.out.persistence.repository;

import com.fixlink.adapter.out.persistence.entity.ServiceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataServiceRepository extends JpaRepository<ServiceJpaEntity, Long> {
    List<ServiceJpaEntity> findByIsActiveTrue();
    List<ServiceJpaEntity> findByCategoryIdAndIsActiveTrue(Long categoryId);
    Optional<ServiceJpaEntity> findByIdAndDeletedAtIsNull(Long id);
}
