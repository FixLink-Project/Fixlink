package com.fixlink.controller;

import com.fixlink.dto.request.*;
import com.fixlink.dto.response.*;
import com.fixlink.service.ServiceCatalogService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RC-4: Admin endpoints for managing service catalog (categories & services).
 */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminServiceCatalogController {

    private final ServiceCatalogService serviceCatalogService;

    public AdminServiceCatalogController(ServiceCatalogService serviceCatalogService) {
        this.serviceCatalogService = serviceCatalogService;
    }

    // ===================================================================
    // Category CRUD
    // ===================================================================

    /**
     * GET /api/v1/admin/categories
     * Admin: Lấy tất cả categories (bao gồm inactive).
     */
    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<ServiceCategoryDto>>> getAllCategories() {
        ApiResponse<List<ServiceCategoryDto>> response =
                serviceCatalogService.getAllCategories();
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/admin/categories/{id}
     * Admin: Chi tiết 1 category.
     */
    @GetMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<ServiceCategoryDto>> getCategoryById(
            @PathVariable Long id) {
        ApiResponse<ServiceCategoryDto> response =
                serviceCatalogService.getCategoryById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/v1/admin/categories
     * Admin: Tạo mới category.
     */
    @PostMapping("/categories")
    public ResponseEntity<ApiResponse<ServiceCategoryDto>> createCategory(
            @Valid @RequestBody CreateCategoryRequest request) {
        ApiResponse<ServiceCategoryDto> response =
                serviceCatalogService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PUT /api/v1/admin/categories/{id}
     * Admin: Cập nhật category.
     */
    @PutMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<ServiceCategoryDto>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCategoryRequest request) {
        ApiResponse<ServiceCategoryDto> response =
                serviceCatalogService.updateCategory(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/v1/admin/categories/{id}
     * Admin: Xóa category (soft delete).
     */
    @DeleteMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {
        ApiResponse<Void> response = serviceCatalogService.deleteCategory(id);
        return ResponseEntity.ok(response);
    }

    // ===================================================================
    // Service CRUD
    // ===================================================================

    /**
     * POST /api/v1/admin/categories/{categoryId}/services
     * Admin: Tạo mới service thuộc category.
     */
    @PostMapping("/categories/{categoryId}/services")
    public ResponseEntity<ApiResponse<ServiceItemDto>> createService(
            @PathVariable Long categoryId,
            @Valid @RequestBody CreateServiceRequest request) {
        // Override categoryId from path
        request.setCategoryId(categoryId);
        ApiResponse<ServiceItemDto> response =
                serviceCatalogService.createService(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/v1/admin/services/{id}
     * Admin: Chi tiết 1 service.
     */
    @GetMapping("/services/{id}")
    public ResponseEntity<ApiResponse<ServiceItemDto>> getServiceById(
            @PathVariable Long id) {
        ApiResponse<ServiceItemDto> response =
                serviceCatalogService.getServiceById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/v1/admin/services/{id}
     * Admin: Cập nhật service.
     */
    @PutMapping("/services/{id}")
    public ResponseEntity<ApiResponse<ServiceItemDto>> updateService(
            @PathVariable Long id,
            @Valid @RequestBody UpdateServiceRequest request) {
        ApiResponse<ServiceItemDto> response =
                serviceCatalogService.updateService(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/v1/admin/services/{id}
     * Admin: Xóa service (soft delete).
     */
    @DeleteMapping("/services/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteService(@PathVariable Long id) {
        ApiResponse<Void> response = serviceCatalogService.deleteService(id);
        return ResponseEntity.ok(response);
    }
}
