package com.fixlink.adapter.in.web.controller;

import com.fixlink.adapter.in.web.dto.request.CategoryRequest;
import com.fixlink.adapter.in.web.dto.response.ApiResponse;
import com.fixlink.adapter.in.web.dto.response.CategoryImpactAssessmentResponse;
import com.fixlink.adapter.in.web.dto.response.CategoryImpactResponse;
import com.fixlink.adapter.in.web.dto.response.CategoryResponse;
import com.fixlink.application.port.in.CategoryAdminUseCase;
import com.fixlink.infrastructure.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Admin Category Management", description = "Các API quản lý danh mục dịch vụ dành cho Quản trị viên")
public class AdminCategoryController {

    private final CategoryAdminUseCase categoryAdminUseCase;

    @GetMapping("/api/v1/admin/categories")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lấy danh sách tất cả danh mục dịch vụ dành cho Quản trị viên")
    public ResponseEntity<ApiResponse<java.util.List<CategoryResponse>>> getAllCategories() {
        java.util.List<CategoryResponse> list = categoryAdminUseCase.getAllCategories();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách danh mục thành công", list));
    }

    @PostMapping("/api/v1/admin/categories")

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo mới danh mục dịch vụ")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CategoryRequest request
    ) {
        Long adminId = principal != null ? principal.getId() : 1L;
        CategoryResponse response = categoryAdminUseCase.createCategory(adminId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Tạo mới danh mục dịch vụ thành công", response));
    }

    @GetMapping("/api/v1/admin/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xem chi tiết danh mục dịch vụ")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryDetail(@PathVariable Long id) {
        CategoryResponse response = categoryAdminUseCase.getCategoryById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin danh mục dịch vụ thành công", response));
    }

    @PutMapping("/api/v1/admin/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật danh mục dịch vụ")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request
    ) {
        Long adminId = principal != null ? principal.getId() : 1L;
        CategoryResponse response = categoryAdminUseCase.updateCategory(adminId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật danh mục dịch vụ thành công", response));
    }

    @DeleteMapping("/api/v1/admin/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa mềm danh mục dịch vụ và đánh giá ảnh hưởng")
    public ResponseEntity<ApiResponse<CategoryImpactResponse>> deleteCategory(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        Long adminId = principal != null ? principal.getId() : 1L;
        CategoryImpactResponse response = categoryAdminUseCase.deleteCategory(adminId, id);
        return ResponseEntity.ok(ApiResponse.success("Xóa mềm danh mục thành công", response));
    }

    @GetMapping({"/api/v1/admin/categories/{id}/impact", "/api/v1/categories/{id}/impact"})
    @Operation(summary = "Đánh giá mức độ ảnh hưởng khi xóa danh mục")
    public ResponseEntity<ApiResponse<CategoryImpactAssessmentResponse>> getCategoryImpact(@PathVariable Long id) {
        CategoryImpactAssessmentResponse response = categoryAdminUseCase.getCategoryImpact(id);
        return ResponseEntity.ok(ApiResponse.success("Đánh giá ảnh hưởng của danh mục thành công", response));
    }
}
