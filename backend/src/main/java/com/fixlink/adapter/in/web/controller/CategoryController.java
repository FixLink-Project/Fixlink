package com.fixlink.adapter.in.web.controller;

import com.fixlink.adapter.in.web.dto.response.ApiResponse;
import com.fixlink.adapter.out.persistence.entity.ServiceCategoryJpaEntity;
import com.fixlink.adapter.out.persistence.entity.ServiceJpaEntity;
import com.fixlink.adapter.out.persistence.repository.SpringDataServiceCategoryRepository;
import com.fixlink.adapter.out.persistence.repository.SpringDataServiceRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Master Data", description = "Các API truy vấn danh mục ngành nghề và dịch vụ sửa chữa")
public class CategoryController {

    private final SpringDataServiceCategoryRepository categoryRepository;
    private final SpringDataServiceRepository serviceRepository;

    public CategoryController(
            SpringDataServiceCategoryRepository categoryRepository,
            SpringDataServiceRepository serviceRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.serviceRepository = serviceRepository;
    }

    @GetMapping("/categories")
    @Operation(summary = "Lấy danh sách danh mục dịch vụ", description = "Truy vấn tất cả danh mục ngành nghề đang hoạt động (Điện lạnh, Điện nước, Gia dụng...).")
    public ResponseEntity<ApiResponse<List<ServiceCategoryJpaEntity>>> getCategories() {
        List<ServiceCategoryJpaEntity> categories = categoryRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách danh mục dịch vụ thành công", categories));
    }

    @GetMapping("/services")
    @Operation(summary = "Lấy danh sách dịch vụ", description = "Truy vấn danh sách gói dịch vụ, hỗ trợ lọc theo categoryId.")
    public ResponseEntity<ApiResponse<List<ServiceJpaEntity>>> getServices(
            @RequestParam(required = false) Long categoryId
    ) {
        List<ServiceJpaEntity> services = (categoryId != null)
                ? serviceRepository.findByCategoryIdAndIsActiveTrue(categoryId)
                : serviceRepository.findByIsActiveTrue();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách dịch vụ thành công", services));
    }
}
