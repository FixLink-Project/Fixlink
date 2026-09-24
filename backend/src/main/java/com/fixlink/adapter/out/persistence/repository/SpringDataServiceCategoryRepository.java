package com.fixlink.adapter.out.persistence.repository;

import com.fixlink.adapter.out.persistence.entity.ServiceCategoryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpringDataServiceCategoryRepository extends JpaRepository<ServiceCategoryJpaEntity, Long> {
    Optional<ServiceCategoryJpaEntity> findByCode(String code);
    List<ServiceCategoryJpaEntity> findByIsActiveTrueOrderByDisplayOrderAsc();

    Optional<ServiceCategoryJpaEntity> findByIdAndDeletedAtIsNull(Long id);
    boolean existsByNameIgnoreCaseAndDeletedAtIsNull(String name);
    boolean existsByNameIgnoreCaseAndIdNotAndDeletedAtIsNull(String name, Long id);
    List<ServiceCategoryJpaEntity> findAllByIsActiveTrueAndDeletedAtIsNull();
    List<ServiceCategoryJpaEntity> findAllByDeletedAtIsNull();

    Page<ServiceCategoryJpaEntity> findAllByDeletedAtIsNull(Pageable pageable);

    Page<ServiceCategoryJpaEntity> findByDeletedAtIsNullAndNameContainingIgnoreCase(String name, Pageable pageable);
}
