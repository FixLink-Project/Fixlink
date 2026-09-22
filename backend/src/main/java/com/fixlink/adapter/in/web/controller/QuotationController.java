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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/repair-requests/{requestId}/quotations")
@RequiredArgsConstructor
@Tag(name = "Quotations", description = "Báo giá cho yêu cầu sửa chữa")
@SecurityRequirement(name = "bearerAuth")
public class QuotationController {

    private final QuotationUseCase quotationUseCase;

    @PostMapping
    @Operation(summary = "Thợ gửi báo giá")
    public ResponseEntity<ApiResponse<QuotationResponse>> create(
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

        QuotationResponse result = quotationUseCase.create(requestId, cmd, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Gửi báo giá thành công", result));
    }

    @PutMapping("/{quotationId}")
    @Operation(summary = "Thợ sửa báo giá")
    public ResponseEntity<ApiResponse<QuotationResponse>> update(
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

        QuotationResponse result = quotationUseCase.update(requestId, quotationId, cmd, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Cập nhật báo giá thành công", result));
    }

    @DeleteMapping("/{quotationId}")
    @Operation(summary = "Thợ rút báo giá")
    public ResponseEntity<ApiResponse<Void>> withdraw(
            @PathVariable Long requestId,
            @PathVariable Long quotationId,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        quotationUseCase.withdraw(requestId, quotationId, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Đã rút báo giá", null));
    }

    @GetMapping
    @Operation(summary = "Khách xem danh sách báo giá cho đơn")
    public ResponseEntity<ApiResponse<List<QuotationResponse>>> getQuotations(
            @PathVariable Long requestId,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        List<QuotationResponse> result = quotationUseCase.getQuotationsForRequest(requestId, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách báo giá thành công", result));
    }

    @PostMapping("/{quotationId}/accept")
    @Operation(summary = "Khách chọn thợ (accept báo giá)")
    public ResponseEntity<ApiResponse<AcceptQuotationResponse>> accept(
            @PathVariable Long requestId,
            @PathVariable Long quotationId,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        AcceptQuotationResponse result = quotationUseCase.accept(requestId, quotationId, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Đã chọn kỹ thuật viên. Vui lòng đặt cọc để xác nhận", result));
    }
}
