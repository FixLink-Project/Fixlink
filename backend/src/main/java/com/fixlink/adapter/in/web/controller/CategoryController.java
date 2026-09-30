package com.fixlink.adapter.in.web.controller;

import com.fixlink.adapter.in.web.dto.response.ApiResponse;
import com.fixlink.adapter.in.web.dto.response.CategoryResponse;
import com.fixlink.adapter.in.web.dto.response.ServiceResponse;
import com.fixlink.application.port.in.MasterDataUseCase;
import com.fixlink.domain.model.Service;
import com.fixlink.domain.model.ServiceCategory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Master Data", description = "Các API truy vấn danh mục ngành nghề và dịch vụ sửa chữa")
public class CategoryController {

    private final MasterDataUseCase masterDataUseCase;

    public CategoryController(MasterDataUseCase masterDataUseCase) {
        this.masterDataUseCase = masterDataUseCase;
    }

    @GetMapping("/categories")
    @Operation(summary = "Lấy danh sách danh mục dịch vụ", description = "Truy vấn tất cả danh mục ngành nghề đang hoạt động (Điện lạnh, Điện nước, Gia dụng...).")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategories() {
        List<CategoryResponse> categories = masterDataUseCase.getActiveCategories().stream()
                .map(this::toCategoryResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách danh mục dịch vụ thành công", categories));
    }

    @GetMapping("/services")
    @Operation(summary = "Lấy danh sách dịch vụ", description = "Truy vấn danh sách gói dịch vụ, hỗ trợ lọc theo categoryId.")
    public ResponseEntity<ApiResponse<List<ServiceResponse>>> getServices(
            @RequestParam(required = false) Long categoryId) {
        List<ServiceResponse> services = masterDataUseCase.getServices(categoryId).stream()
                .map(this::toServiceResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách dịch vụ thành công", services));
    }

    private CategoryResponse toCategoryResponse(ServiceCategory c) {
        return CategoryResponse.builder()
                .id(c.getId())
                .code(c.getCode())
                .name(c.getName())
                .description(c.getDescription())
                .iconUrl(c.getIconUrl())
                .displayOrder(c.getDisplayOrder())
                .isActive(c.getIsActive())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }

    private ServiceResponse toServiceResponse(Service s) {
        return ServiceResponse.builder()
                .id(s.getId())
                .categoryId(s.getCategoryId())
                .code(s.getCode())
                .name(s.getName())
                .description(s.getDescription())
                .basePrice(s.getBasePrice())
                .estimatedDurationMinutes(s.getEstimatedDurationMinutes())
                .isActive(s.getIsActive())
                .build();
    }
}
