package com.fixlink.adapter.in.web.controller;

import com.fixlink.adapter.in.web.dto.response.ApiResponse;
import com.fixlink.adapter.in.web.dto.response.QuotationResponse;
import com.fixlink.application.port.in.QuotationUseCase;
import com.fixlink.infrastructure.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Bản ghi nhận việc của một yêu cầu sửa chữa.
 *
 * <p><b>Đã pivot sang mô hình "ai nhận trước được trước".</b> Các endpoint của
 * mô hình đấu giá ngược cũ đã được bỏ khỏi API:
 * <ul>
 *   <li>{@code POST   .../quotations}               — thợ gửi báo giá</li>
 *   <li>{@code PUT    .../quotations/{id}}          — thợ sửa báo giá</li>
 *   <li>{@code DELETE .../quotations/{id}}          — thợ rút báo giá</li>
 *   <li>{@code POST   .../quotations/{id}/accept}   — khách chọn thợ</li>
 * </ul>
 * Thay thế bằng một endpoint duy nhất:
 * {@code POST /api/v1/repair-requests/{requestId}/apply} (xem
 * {@link RepairRequestController}). Phần code service của luồng cũ vẫn còn
 * trong {@code QuotationService} (đánh dấu {@code @Deprecated}) để đối chiếu,
 * nhưng không còn đường vào từ HTTP.
 */
@RestController
@RequestMapping("/api/v1/repair-requests/{requestId}/quotations")
@RequiredArgsConstructor
@Tag(name = "Quotations", description = "Bản ghi thợ nhận việc")
@SecurityRequirement(name = "bearerAuth")
public class QuotationController {

    private final QuotationUseCase quotationUseCase;

    /**
     * Sau pivot, danh sách này trả về tối đa một bản ghi — chính là thợ đã nhận
     * việc. Dùng để hiển thị "đã có thợ nhận" trên trang chi tiết yêu cầu.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @Operation(summary = "Xem thợ đã nhận việc của yêu cầu",
            description = "Trả về bản ghi nhận việc (tối đa 1) của yêu cầu sửa chữa.")
    public ResponseEntity<ApiResponse<List<QuotationResponse>>> getQuotations(
            @PathVariable Long requestId,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        List<QuotationResponse> result = quotationUseCase.getQuotationsForRequest(requestId, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin thợ nhận việc thành công", result));
    }
}
