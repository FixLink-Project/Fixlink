package com.fixlink.adapter.in.web.controller;

import com.fixlink.adapter.in.web.dto.response.ApiResponse;
import com.fixlink.adapter.in.web.dto.response.QuotationResponse;
import com.fixlink.application.port.in.QuotationUseCase;
import com.fixlink.domain.model.QuotationStatus;
import com.fixlink.infrastructure.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller xử lý danh sách báo giá của Kỹ thuật viên (Feature RC-42).
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Technician Quotations", description = "Quản lý danh sách báo giá của kỹ thuật viên (RC-42)")
@SecurityRequirement(name = "bearerAuth")
public class TechnicianQuotationController {

    private final QuotationUseCase quotationUseCase;

    @GetMapping("/technicians/me/quotations")
    @PreAuthorize("hasRole('TECHNICIAN')")
    @Operation(summary = "Kỹ thuật viên xem danh sách báo giá của mình (RC-42)")
    public ResponseEntity<ApiResponse<List<QuotationResponse>>> getMyQuotations(
            @RequestParam(required = false) QuotationStatus status,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        List<QuotationResponse> result = quotationUseCase.getMyQuotations(user.getId(), status);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách báo giá của thợ thành công", result));
    }

    @GetMapping("/technicians/me/quotations/{id}")
    @PreAuthorize("hasRole('TECHNICIAN')")
    @Operation(summary = "Kỹ thuật viên xem chi tiết một báo giá của mình (RC-42)")
    public ResponseEntity<ApiResponse<QuotationResponse>> getMyQuotationDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        QuotationResponse result = quotationUseCase.getQuotationByIdForTechnician(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin báo giá thành công", result));
    }

    @GetMapping("/quotations/my")
    @PreAuthorize("hasRole('TECHNICIAN')")
    @Operation(summary = "Kỹ thuật viên xem danh sách báo giá của mình (alias RC-42)")
    public ResponseEntity<ApiResponse<List<QuotationResponse>>> getMyQuotationsAlias(
            @RequestParam(required = false) QuotationStatus status,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        return getMyQuotations(status, user);
    }
}
