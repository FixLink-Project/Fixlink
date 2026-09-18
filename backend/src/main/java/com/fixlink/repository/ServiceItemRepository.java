package com.fixlink.repository;

import com.fixlink.entity.ServiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceItemRepository extends JpaRepository<ServiceItem, Long> {

    List<ServiceItem> findAllByCategoryIdAndDeletedAtIsNullOrderBySortOrderAsc(Long categoryId);

    List<ServiceItem> findAllByCategoryIdAndDeletedAtIsNullAndIsActiveTrueOrderBySortOrderAsc(Long categoryId);

    Optional<ServiceItem> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByCategoryIdAndNameAndDeletedAtIsNull(Long categoryId, String name);

    boolean existsByCategoryIdAndNameAndDeletedAtIsNullAndIdNot(Long categoryId, String name, Long id);

    long countByCategoryIdAndDeletedAtIsNullAndIsActiveTrue(Long categoryId);
}
