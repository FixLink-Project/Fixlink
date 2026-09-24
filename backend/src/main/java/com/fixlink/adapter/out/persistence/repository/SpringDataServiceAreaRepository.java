package com.fixlink.adapter.out.persistence.repository;

import com.fixlink.adapter.out.persistence.entity.ServiceAreaJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataServiceAreaRepository extends JpaRepository<ServiceAreaJpaEntity, Long> {
    List<ServiceAreaJpaEntity> findAllByIsActiveTrue();
    Optional<ServiceAreaJpaEntity> findByIdAndIsActiveTrue(Long id);
    Optional<ServiceAreaJpaEntity> findByIdAndDeletedAtIsNull(Long id);
}
