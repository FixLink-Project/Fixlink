package com.fixlink.application.port.in;

import com.fixlink.adapter.in.web.dto.request.CategoryRequest;
import com.fixlink.adapter.in.web.dto.response.CategoryImpactAssessmentResponse;
import com.fixlink.adapter.in.web.dto.response.CategoryImpactResponse;
import com.fixlink.adapter.in.web.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryAdminUseCase {
    List<CategoryResponse> getPublicCategories();
    List<CategoryResponse> getAllCategories();
    CategoryResponse createCategory(Long adminId, CategoryRequest request);
    CategoryResponse getCategoryById(Long id);
    CategoryResponse updateCategory(Long adminId, Long id, CategoryRequest request);
    CategoryImpactResponse deleteCategory(Long adminId, Long id);
    CategoryImpactAssessmentResponse getCategoryImpact(Long id);
}
