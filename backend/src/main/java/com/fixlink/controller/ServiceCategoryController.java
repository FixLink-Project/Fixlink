package com.fixlink.controller;

import com.fixlink.dto.response.ApiResponse;
import com.fixlink.dto.response.ServiceCategoryDto;
import com.fixlink.service.ServiceCatalogService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RC-4: Public endpoints for browsing service catalog.
 * Customers use these when creating a repair request.
 */
@RestController
@RequestMapping("/api/v1/categories")
public class ServiceCategoryController {

    private final ServiceCatalogService serviceCatalogService;

    public ServiceCategoryController(ServiceCatalogService serviceCatalogService) {
        this.serviceCatalogService = serviceCatalogService;
    }

    /**
     * GET /api/v1/categories
     * Lấy danh sách tất cả danh mục dịch vụ (active) + services.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ServiceCategoryDto>>> getAllCategories() {
        ApiResponse<List<ServiceCategoryDto>> response =
                serviceCatalogService.getAllActiveCategories();
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/categories/{id}
     * Chi tiết 1 danh mục dịch vụ + danh sách services.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ServiceCategoryDto>> getCategoryById(
            @PathVariable Long id) {
        ApiResponse<ServiceCategoryDto> response =
                serviceCatalogService.getActiveCategoryById(id);
        return ResponseEntity.ok(response);
    }
}
