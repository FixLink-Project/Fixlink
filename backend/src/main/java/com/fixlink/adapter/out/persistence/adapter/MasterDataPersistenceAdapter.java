package com.fixlink.adapter.out.persistence.adapter;

import com.fixlink.application.port.in.MasterDataUseCase.ServiceAreaView;
import com.fixlink.application.port.out.MasterDataRepositoryPort;
import com.fixlink.adapter.out.persistence.entity.ServiceAreaJpaEntity;
import com.fixlink.adapter.out.persistence.entity.ServiceCategoryJpaEntity;
import com.fixlink.adapter.out.persistence.entity.ServiceJpaEntity;
import com.fixlink.adapter.out.persistence.repository.SpringDataServiceAreaRepository;
import com.fixlink.adapter.out.persistence.repository.SpringDataServiceCategoryRepository;
import com.fixlink.adapter.out.persistence.repository.SpringDataServiceRepository;
import com.fixlink.domain.model.Service;
import com.fixlink.domain.model.ServiceCategory;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Adapter ra (adapter out) hiện thực {@link MasterDataRepositoryPort}: gọi Spring
 * Data repository rồi map JPA entity ↔ domain model. Đây là nơi duy nhất trong
 * luồng master data được phép biết tới JPA entity.
 */
@Component
public class MasterDataPersistenceAdapter implements MasterDataRepositoryPort {

    private final SpringDataServiceCategoryRepository categoryRepository;
    private final SpringDataServiceRepository serviceRepository;
    private final SpringDataServiceAreaRepository serviceAreaRepository;

    public MasterDataPersistenceAdapter(
            SpringDataServiceCategoryRepository categoryRepository,
            SpringDataServiceRepository serviceRepository,
            SpringDataServiceAreaRepository serviceAreaRepository) {
        this.categoryRepository = categoryRepository;
        this.serviceRepository = serviceRepository;
        this.serviceAreaRepository = serviceAreaRepository;
    }

    @Override
    public List<ServiceCategory> findActiveCategories() {
        return categoryRepository.findByIsActiveTrueOrderByDisplayOrderAsc().stream()
                .map(this::toCategoryDomain)
                .toList();
    }

    @Override
    public List<Service> findServices(Long categoryId) {
        List<ServiceJpaEntity> entities = (categoryId != null)
                ? serviceRepository.findByCategoryIdAndIsActiveTrue(categoryId)
                : serviceRepository.findByIsActiveTrue();
        return entities.stream().map(this::toServiceDomain).toList();
    }

    @Override
    public List<ServiceAreaView> findActiveAreas() {
        return serviceAreaRepository.findAllByIsActiveTrue().stream()
                .map(this::toAreaView)
                .toList();
    }

    private ServiceCategory toCategoryDomain(ServiceCategoryJpaEntity e) {
        return ServiceCategory.builder()
                .id(e.getId())
                .code(e.getCode())
                .name(e.getName())
                .description(e.getDescription())
                .iconUrl(e.getIconUrl())
                .displayOrder(e.getDisplayOrder())
                .isActive(e.getIsActive())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    private Service toServiceDomain(ServiceJpaEntity e) {
        return Service.builder()
                .id(e.getId())
                .categoryId(e.getCategoryId())
                .code(e.getCode())
                .name(e.getName())
                .description(e.getDescription())
                .basePrice(e.getBasePrice())
                .estimatedDurationMinutes(e.getEstimatedDurationMinutes())
                .isActive(e.getIsActive())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    private ServiceAreaView toAreaView(ServiceAreaJpaEntity e) {
        return new ServiceAreaView(e.getId(), e.getCode(), e.getName(), e.getCity());
    }
}
