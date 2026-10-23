package com.fixlink.adapter.in.web.controller;

import com.fixlink.adapter.in.web.dto.request.CreateQuotationRequest;
import com.fixlink.adapter.in.web.dto.request.UpdateQuotationRequest;
import com.fixlink.adapter.in.web.dto.response.AcceptQuotationResponse;
import com.fixlink.adapter.in.web.dto.response.ApiResponse;
import com.fixlink.adapter.in.web.dto.response.QuotationResponse;
import com.fixlink.application.port.in.QuotationUseCase;
import com.fixlink.infrastructure.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller quản lý Báo giá & Đấu giá cạnh tranh giữa các thợ.
 */
@RestController
@RequestMapping("/api/v1/repair-requests/{requestId}/quotations")
@RequiredArgsConstructor
@Tag(name = "Quotations", description = "Báo giá & Đấu giá cạnh tranh")
@SecurityRequirement(name = "bearerAuth")
public class QuotationController {

    private final QuotationUseCase quotationUseCase;

    @PostMapping
    @PreAuthorize("hasRole('TECHNICIAN')")
    @Operation(summary = "Thợ gửi báo giá / trả giá cho yêu cầu sửa chữa")
    public ResponseEntity<ApiResponse<QuotationResponse>> createQuotation(
            @PathVariable Long requestId,
            @Valid @RequestBody CreateQuotationRequest req,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        var cmd = QuotationUseCase.CreateQuotationCommand.builder()
                .solution(req.getSolution())
                .priceLaborVnd(req.getPriceLaborVnd())
                .priceMaterialsVnd(req.getPriceMaterialsVnd())
                .inspectionTime(req.getInspectionTime())
                .estimatedFinish(req.getEstimatedFinish())
                .note(req.getNote())
                .build();
        QuotationResponse res = quotationUseCase.create(requestId, cmd, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created("Gửi báo giá thành công", res));
    }

    @PutMapping("/{quotationId}")
    @PreAuthorize("hasRole('TECHNICIAN')")
    @Operation(summary = "Thợ cập nhật báo giá / trả giá lại")
    public ResponseEntity<ApiResponse<QuotationResponse>> updateQuotation(
            @PathVariable Long requestId,
            @PathVariable Long quotationId,
            @Valid @RequestBody UpdateQuotationRequest req,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        var cmd = QuotationUseCase.UpdateQuotationCommand.builder()
                .solution(req.getSolution())
                .priceLaborVnd(req.getPriceLaborVnd())
                .priceMaterialsVnd(req.getPriceMaterialsVnd())
                .inspectionTime(req.getInspectionTime())
                .estimatedFinish(req.getEstimatedFinish())
                .note(req.getNote())
                .build();
        QuotationResponse res = quotationUseCase.update(requestId, quotationId, cmd, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Cập nhật báo giá thành công", res));
    }

    @DeleteMapping("/{quotationId}")
    @PreAuthorize("hasRole('TECHNICIAN')")
    @Operation(summary = "Thợ rút báo giá")
    public ResponseEntity<ApiResponse<Void>> withdrawQuotation(
            @PathVariable Long requestId,
            @PathVariable Long quotationId,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        quotationUseCase.withdraw(requestId, quotationId, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Đã rút báo giá thành công", null));
    }

    @PostMapping("/{quotationId}/accept")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Khách hàng chọn thợ (chấp nhận báo giá)")
    public ResponseEntity<ApiResponse<AcceptQuotationResponse>> acceptQuotation(
            @PathVariable Long requestId,
            @PathVariable Long quotationId,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        AcceptQuotationResponse res = quotationUseCase.accept(requestId, quotationId, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Đã chọn thợ và chốt báo giá thành công", res));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'TECHNICIAN', 'ADMIN')")
    @Operation(summary = "Xem danh sách báo giá của yêu cầu",
            description = "Khách hàng/Admin xem tất cả báo giá. Thợ chỉ xem báo giá của chính mình (đấu thầu kín).")
    public ResponseEntity<ApiResponse<List<QuotationResponse>>> getQuotations(
            @PathVariable Long requestId,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        List<QuotationResponse> result = quotationUseCase.getQuotationsForRequest(requestId, user.getId(), user.getRole().name());
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách báo giá thành công", result));
    }
}
