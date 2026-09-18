package com.fixlink.repository;

import com.fixlink.entity.ServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceCategoryRepository extends JpaRepository<ServiceCategory, Long> {

    List<ServiceCategory> findAllByDeletedAtIsNullOrderBySortOrderAsc();

    List<ServiceCategory> findAllByDeletedAtIsNullAndIsActiveTrueOrderBySortOrderAsc();

    Optional<ServiceCategory> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByNameAndDeletedAtIsNull(String name);

    boolean existsByNameAndDeletedAtIsNullAndIdNot(String name, Long id);
}
