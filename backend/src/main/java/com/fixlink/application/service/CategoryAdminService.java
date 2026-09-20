package com.fixlink.application.service;

import com.fixlink.adapter.in.web.dto.request.CategoryRequest;
import com.fixlink.adapter.in.web.dto.response.CategoryImpactAssessmentResponse;
import com.fixlink.adapter.in.web.dto.response.CategoryImpactResponse;
import com.fixlink.adapter.in.web.dto.response.CategoryResponse;
import com.fixlink.adapter.out.persistence.entity.ServiceCategoryJpaEntity;
import com.fixlink.adapter.out.persistence.repository.SpringDataRepairRequestRepository;
import com.fixlink.adapter.out.persistence.repository.SpringDataServiceCategoryRepository;
import com.fixlink.adapter.out.persistence.repository.SpringDataTechnicianProfileRepository;
import com.fixlink.application.port.in.CategoryAdminUseCase;
import com.fixlink.domain.exception.DuplicateCategoryNameException;
import com.fixlink.domain.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryAdminService implements CategoryAdminUseCase {

    private final SpringDataServiceCategoryRepository serviceCategoryRepository;
    private final SpringDataTechnicianProfileRepository technicianProfileRepository;
    private final SpringDataRepairRequestRepository repairRequestRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getPublicCategories() {
        return serviceCategoryRepository.findAllByIsActiveTrueAndDeletedAtIsNull()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return serviceCategoryRepository.findAllByDeletedAtIsNull()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }


    @Override
    @Transactional
    public CategoryResponse createCategory(Long adminId, CategoryRequest request) {
        String trimmedName = request.getName().trim();
        if (serviceCategoryRepository.existsByNameIgnoreCaseAndDeletedAtIsNull(trimmedName)) {
            Map<String, String> errors = new HashMap<>();
            errors.put("name", "Danh mục '" + trimmedName + "' đã tồn tại");
            throw new DuplicateCategoryNameException("Tên danh mục đã tồn tại trong hệ thống", errors);
        }

        ServiceCategoryJpaEntity category = new ServiceCategoryJpaEntity();
        category.setName(trimmedName);
        category.setCode(request.getCode() != null ? request.getCode() : "CAT_" + System.currentTimeMillis());
        category.setDescription(request.getDescription());
        category.setIconUrl(request.getIconUrl());
        category.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
        category.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        category.setCreatedBy(adminId);
        category.setUpdatedBy(adminId);

        category = serviceCategoryRepository.save(category);
        return mapToResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        ServiceCategoryJpaEntity category = serviceCategoryRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục dịch vụ với ID = " + id));
        return mapToResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryImpactAssessmentResponse getCategoryImpact(Long id) {
        ServiceCategoryJpaEntity category = serviceCategoryRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục dịch vụ với ID = " + id));

        long techCount = 0; // count of linked technicians
        long pendingRequestsCount = repairRequestRepository.countByCategoryId(id);

        return CategoryImpactAssessmentResponse.builder()
                .categoryId(category.getId())
                .categoryName(category.getName())
                .activeTechniciansCount(techCount)
                .pendingRequestsCount(pendingRequestsCount)
                .canDeleteDirectly(techCount == 0 && pendingRequestsCount == 0)
                .build();
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long adminId, Long id, CategoryRequest request) {
        ServiceCategoryJpaEntity category = serviceCategoryRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục dịch vụ với ID = " + id));

        String newName = request.getName().trim();
        if (serviceCategoryRepository.existsByNameIgnoreCaseAndIdNotAndDeletedAtIsNull(newName, id)) {
            Map<String, String> errors = new HashMap<>();
            errors.put("name", "Danh mục '" + newName + "' đã tồn tại");
            throw new DuplicateCategoryNameException("Tên danh mục đã tồn tại trong hệ thống", errors);
        }

        category.setName(newName);
        if (request.getCode() != null) {
            category.setCode(request.getCode());
        }
        if (request.getIconUrl() != null) {
            category.setIconUrl(request.getIconUrl());
        }
        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }
        if (request.getDisplayOrder() != null) {
            category.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getIsActive() != null) {
            category.setIsActive(request.getIsActive());
        }

        category.setUpdatedBy(adminId);
        category.setUpdatedAt(LocalDateTime.now());

        category = serviceCategoryRepository.save(category);
        return mapToResponse(category);
    }

    @Override
    @Transactional
    public CategoryImpactResponse deleteCategory(Long adminId, Long id) {
        ServiceCategoryJpaEntity category = serviceCategoryRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục dịch vụ với ID = " + id));

        long techCount = 0;
        long pendingRequestsCount = repairRequestRepository.countByCategoryId(id);

        LocalDateTime now = LocalDateTime.now();
        category.setDeletedAt(now);
        category.setDeletedBy(adminId);
        category.setIsActive(false);
        category.setUpdatedAt(now);
        category.setUpdatedBy(adminId);

        serviceCategoryRepository.save(category);

        return CategoryImpactResponse.builder()
                .categoryId(id)
                .categoryName(category.getName())
                .affectedTechnicians(techCount)
                .affectedPendingRequests(pendingRequestsCount)
                .message("Xóa mềm danh mục thành công")
                .build();
    }

    private CategoryResponse mapToResponse(ServiceCategoryJpaEntity entity) {
        return CategoryResponse.builder()
                .id(entity.getId())
                .code(entity.getCode())
                .name(entity.getName())
                .description(entity.getDescription())
                .iconUrl(entity.getIconUrl())
                .displayOrder(entity.getDisplayOrder())
                .isActive(entity.getIsActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
