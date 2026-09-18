package com.fixlink.service;

import com.fixlink.dto.request.*;
import com.fixlink.dto.response.*;
import com.fixlink.entity.ServiceCategory;
import com.fixlink.entity.ServiceItem;
import com.fixlink.exception.InvalidOperationException;
import com.fixlink.exception.ResourceNotFoundException;
import com.fixlink.repository.ServiceCategoryRepository;
import com.fixlink.repository.ServiceItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ServiceCatalogService {

    private final ServiceCategoryRepository categoryRepository;
    private final ServiceItemRepository serviceItemRepository;

    public ServiceCatalogService(ServiceCategoryRepository categoryRepository,
                                  ServiceItemRepository serviceItemRepository) {
        this.categoryRepository = categoryRepository;
        this.serviceItemRepository = serviceItemRepository;
    }

    // ===================================================================
    // Category — Public read operations
    // ===================================================================

    /**
     * Lấy tất cả category active (public, cho khách hàng duyệt).
     * Mỗi category kèm danh sách services active.
     */
    @Transactional(readOnly = true)
    public ApiResponse<List<ServiceCategoryDto>> getAllActiveCategories() {
        List<ServiceCategory> categories =
                categoryRepository.findAllByDeletedAtIsNullAndIsActiveTrueOrderBySortOrderAsc();

        List<ServiceCategoryDto> dtos = categories.stream()
                .map(cat -> toCategoryDto(cat, true, true))
                .collect(Collectors.toList());

        return ApiResponse.success("Lấy danh sách danh mục dịch vụ thành công", dtos);
    }

    /**
     * Chi tiết 1 category + services (public).
     */
    @Transactional(readOnly = true)
    public ApiResponse<ServiceCategoryDto> getActiveCategoryById(Long id) {
        ServiceCategory category = categoryRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy danh mục dịch vụ với ID: " + id));

        if (!category.getIsActive()) {
            throw new ResourceNotFoundException("Danh mục dịch vụ không khả dụng");
        }

        return ApiResponse.success("Thành công", toCategoryDto(category, true, true));
    }

    // ===================================================================
    // Category — Admin CRUD operations
    // ===================================================================

    /**
     * Admin: Lấy tất cả category (bao gồm inactive).
     */
    @Transactional(readOnly = true)
    public ApiResponse<List<ServiceCategoryDto>> getAllCategories() {
        List<ServiceCategory> categories =
                categoryRepository.findAllByDeletedAtIsNullOrderBySortOrderAsc();

        List<ServiceCategoryDto> dtos = categories.stream()
                .map(cat -> toCategoryDto(cat, true, false))
                .collect(Collectors.toList());

        return ApiResponse.success("Lấy danh sách danh mục dịch vụ thành công", dtos);
    }

    /**
     * Admin: Chi tiết 1 category.
     */
    @Transactional(readOnly = true)
    public ApiResponse<ServiceCategoryDto> getCategoryById(Long id) {
        ServiceCategory category = categoryRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy danh mục dịch vụ với ID: " + id));

        return ApiResponse.success("Thành công", toCategoryDto(category, true, false));
    }

    /**
     * Admin: Tạo mới category.
     */
    @Transactional
    public ApiResponse<ServiceCategoryDto> createCategory(CreateCategoryRequest request) {
        // Kiểm tra trùng tên
        if (categoryRepository.existsByNameAndDeletedAtIsNull(request.getName().trim())) {
            throw new InvalidOperationException(
                    "Danh mục dịch vụ với tên '" + request.getName().trim() + "' đã tồn tại",
                    "DUPLICATE_NAME");
        }

        ServiceCategory category = ServiceCategory.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .iconUrl(request.getIconUrl())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .isActive(true)
                .build();

        categoryRepository.save(category);

        return ApiResponse.created("Tạo danh mục dịch vụ thành công",
                toCategoryDto(category, false, false));
    }

    /**
     * Admin: Cập nhật category.
     */
    @Transactional
    public ApiResponse<ServiceCategoryDto> updateCategory(Long id, UpdateCategoryRequest request) {
        ServiceCategory category = categoryRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy danh mục dịch vụ với ID: " + id));

        // Kiểm tra trùng tên (nếu đổi tên)
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            if (categoryRepository.existsByNameAndDeletedAtIsNullAndIdNot(
                    request.getName().trim(), id)) {
                throw new InvalidOperationException(
                        "Danh mục dịch vụ với tên '" + request.getName().trim() + "' đã tồn tại",
                        "DUPLICATE_NAME");
            }
            category.setName(request.getName().trim());
        }

        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }
        if (request.getIconUrl() != null) {
            category.setIconUrl(request.getIconUrl());
        }
        if (request.getSortOrder() != null) {
            category.setSortOrder(request.getSortOrder());
        }
        if (request.getIsActive() != null) {
            category.setIsActive(request.getIsActive());
        }

        categoryRepository.save(category);

        return ApiResponse.success("Cập nhật danh mục dịch vụ thành công",
                toCategoryDto(category, false, false));
    }

    /**
     * Admin: Soft delete category.
     * Chỉ cho phép xóa khi không còn service active.
     */
    @Transactional
    public ApiResponse<Void> deleteCategory(Long id) {
        ServiceCategory category = categoryRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy danh mục dịch vụ với ID: " + id));

        // Kiểm tra còn service active không
        long activeServiceCount = serviceItemRepository
                .countByCategoryIdAndDeletedAtIsNullAndIsActiveTrue(id);
        if (activeServiceCount > 0) {
            throw new InvalidOperationException(
                    "Không thể xóa danh mục đang có " + activeServiceCount
                            + " dịch vụ hoạt động. Vui lòng xóa hoặc vô hiệu hóa các dịch vụ trước.",
                    "CATEGORY_HAS_ACTIVE_SERVICES");
        }

        category.setDeletedAt(LocalDateTime.now());
        categoryRepository.save(category);

        return ApiResponse.success("Xóa danh mục dịch vụ thành công", null);
    }

    // ===================================================================
    // Service — Admin CRUD operations
    // ===================================================================

    /**
     * Admin: Tạo mới service.
     */
    @Transactional
    public ApiResponse<ServiceItemDto> createService(CreateServiceRequest request) {
        // Validate category tồn tại
        ServiceCategory category = categoryRepository
                .findByIdAndDeletedAtIsNull(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy danh mục dịch vụ với ID: " + request.getCategoryId()));

        // Kiểm tra trùng tên trong cùng category
        if (serviceItemRepository.existsByCategoryIdAndNameAndDeletedAtIsNull(
                request.getCategoryId(), request.getName().trim())) {
            throw new InvalidOperationException(
                    "Dịch vụ với tên '" + request.getName().trim()
                            + "' đã tồn tại trong danh mục này",
                    "DUPLICATE_NAME");
        }

        ServiceItem serviceItem = ServiceItem.builder()
                .category(category)
                .name(request.getName().trim())
                .description(request.getDescription())
                .estimatedPrice(request.getEstimatedPrice())
                .unit(request.getUnit())
                .iconUrl(request.getIconUrl())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .isActive(true)
                .build();

        serviceItemRepository.save(serviceItem);

        return ApiResponse.created("Tạo dịch vụ thành công", toServiceItemDto(serviceItem));
    }

    /**
     * Admin: Chi tiết 1 service.
     */
    @Transactional(readOnly = true)
    public ApiResponse<ServiceItemDto> getServiceById(Long id) {
        ServiceItem serviceItem = serviceItemRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy dịch vụ với ID: " + id));

        return ApiResponse.success("Thành công", toServiceItemDto(serviceItem));
    }

    /**
     * Admin: Cập nhật service.
     */
    @Transactional
    public ApiResponse<ServiceItemDto> updateService(Long id, UpdateServiceRequest request) {
        ServiceItem serviceItem = serviceItemRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy dịch vụ với ID: " + id));

        // Nếu đổi category
        if (request.getCategoryId() != null) {
            ServiceCategory newCategory = categoryRepository
                    .findByIdAndDeletedAtIsNull(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy danh mục dịch vụ với ID: " + request.getCategoryId()));
            serviceItem.setCategory(newCategory);
        }

        // Kiểm tra trùng tên (nếu đổi tên hoặc đổi category)
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            Long categoryId = request.getCategoryId() != null
                    ? request.getCategoryId() : serviceItem.getCategory().getId();
            if (serviceItemRepository.existsByCategoryIdAndNameAndDeletedAtIsNullAndIdNot(
                    categoryId, request.getName().trim(), id)) {
                throw new InvalidOperationException(
                        "Dịch vụ với tên '" + request.getName().trim()
                                + "' đã tồn tại trong danh mục này",
                        "DUPLICATE_NAME");
            }
            serviceItem.setName(request.getName().trim());
        }

        if (request.getDescription() != null) {
            serviceItem.setDescription(request.getDescription());
        }
        if (request.getEstimatedPrice() != null) {
            serviceItem.setEstimatedPrice(request.getEstimatedPrice());
        }
        if (request.getUnit() != null) {
            serviceItem.setUnit(request.getUnit());
        }
        if (request.getIconUrl() != null) {
            serviceItem.setIconUrl(request.getIconUrl());
        }
        if (request.getSortOrder() != null) {
            serviceItem.setSortOrder(request.getSortOrder());
        }
        if (request.getIsActive() != null) {
            serviceItem.setIsActive(request.getIsActive());
        }

        serviceItemRepository.save(serviceItem);

        return ApiResponse.success("Cập nhật dịch vụ thành công", toServiceItemDto(serviceItem));
    }

    /**
     * Admin: Soft delete service.
     */
    @Transactional
    public ApiResponse<Void> deleteService(Long id) {
        ServiceItem serviceItem = serviceItemRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy dịch vụ với ID: " + id));

        serviceItem.setDeletedAt(LocalDateTime.now());
        serviceItemRepository.save(serviceItem);

        return ApiResponse.success("Xóa dịch vụ thành công", null);
    }

    // ===================================================================
    // Helper methods: Entity → DTO mapping
    // ===================================================================

    /**
     * Convert ServiceCategory → ServiceCategoryDto.
     *
     * @param category       the entity
     * @param includeServices whether to include nested services list
     * @param activeOnly      if true, only include active services (for public API)
     */
    private ServiceCategoryDto toCategoryDto(ServiceCategory category,
                                              boolean includeServices,
                                              boolean activeOnly) {
        ServiceCategoryDto.ServiceCategoryDtoBuilder builder = ServiceCategoryDto.builder()
                .id("cat_" + category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .iconUrl(category.getIconUrl())
                .sortOrder(category.getSortOrder())
                .isActive(category.getIsActive())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt());

        if (includeServices) {
            List<ServiceItem> services = activeOnly
                    ? serviceItemRepository
                        .findAllByCategoryIdAndDeletedAtIsNullAndIsActiveTrueOrderBySortOrderAsc(
                                category.getId())
                    : serviceItemRepository
                        .findAllByCategoryIdAndDeletedAtIsNullOrderBySortOrderAsc(
                                category.getId());

            builder.serviceCount(services.size());
            builder.services(services.stream()
                    .map(this::toServiceItemDto)
                    .collect(Collectors.toList()));
        }

        return builder.build();
    }

    /**
     * Convert ServiceItem → ServiceItemDto.
     */
    private ServiceItemDto toServiceItemDto(ServiceItem item) {
        return ServiceItemDto.builder()
                .id("svc_" + item.getId())
                .categoryId("cat_" + item.getCategory().getId())
                .categoryName(item.getCategory().getName())
                .name(item.getName())
                .description(item.getDescription())
                .estimatedPrice(item.getEstimatedPrice())
                .unit(item.getUnit())
                .iconUrl(item.getIconUrl())
                .sortOrder(item.getSortOrder())
                .isActive(item.getIsActive())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}
