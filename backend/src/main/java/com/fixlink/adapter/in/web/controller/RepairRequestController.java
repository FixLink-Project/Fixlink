package com.fixlink.adapter.in.web.controller;

import com.fixlink.adapter.in.web.dto.request.CancelRepairRequestRequest;
import com.fixlink.adapter.in.web.dto.request.CreateRepairRequestRequest;
import com.fixlink.adapter.in.web.dto.request.UpdateRepairRequestRequest;
import com.fixlink.adapter.in.web.dto.response.*;
import com.fixlink.application.port.in.RepairRequestUseCase;
import com.fixlink.infrastructure.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/repair-requests")
@RequiredArgsConstructor
@Tag(name = "Repair Requests", description = "CRUD yêu cầu sửa chữa")
@SecurityRequirement(name = "bearerAuth")
public class RepairRequestController {

    private final RepairRequestUseCase repairRequestUseCase;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Tạo yêu cầu sửa chữa mới")
    public ResponseEntity<ApiResponse<RepairRequestResponse>> create(
            @Valid @RequestBody CreateRepairRequestRequest req,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        var cmd = RepairRequestUseCase.CreateCommand.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .categoryId(req.getCategoryId())
                .areaId(req.getAreaId())
                .addressLine(req.getAddressLine())
                .latitude(req.getLatitude())
                .longitude(req.getLongitude())
                .preferredTime(req.getPreferredTime())
                .budgetRef(req.getBudgetRef())
                .biddingDeadlineDays(req.getBiddingDeadlineDays())
                .mediaUrls(req.getMediaUrls())
                .build();

        RepairRequestResponse result = repairRequestUseCase.create(cmd, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Tạo yêu cầu sửa chữa thành công", result));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Danh sách yêu cầu của tôi (khách hàng)")
    public ResponseEntity<ApiPageResponse<RepairRequestResponse>> getMyRequests(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortOrder
    ) {
        Page<RepairRequestResponse> pageResult = repairRequestUseCase.getMyRequests(
                user.getId(), status, page, limit, sortBy, sortOrder);

        ApiPageResponse<RepairRequestResponse> response = ApiPageResponse.of(
                "Lấy danh sách yêu cầu thành công",
                page,
                limit,
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                pageResult.getContent()
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết yêu cầu sửa chữa")
    public ResponseEntity<ApiResponse<RepairRequestDetailResponse>> getDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        RepairRequestDetailResponse detail = repairRequestUseCase.getDetail(
                id, user.getId(), user.getRole().name());
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết yêu cầu thành công", detail));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Sửa yêu cầu (chỉ khi DRAFT hoặc BIDDING_OPEN chưa có báo giá)")
    public ResponseEntity<ApiResponse<RepairRequestResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRepairRequestRequest req,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        var cmd = RepairRequestUseCase.UpdateCommand.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .categoryId(req.getCategoryId())
                .areaId(req.getAreaId())
                .addressLine(req.getAddressLine())
                .latitude(req.getLatitude())
                .longitude(req.getLongitude())
                .preferredTime(req.getPreferredTime())
                .budgetRef(req.getBudgetRef())
                .biddingDeadlineDays(req.getBiddingDeadlineDays())
                .mediaUrls(req.getMediaUrls())
                .build();

        RepairRequestResponse result = repairRequestUseCase.update(id, cmd, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Cập nhật yêu cầu thành công", result));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Hủy yêu cầu sửa chữa")
    public ResponseEntity<ApiResponse<RepairRequestResponse>> cancel(
            @PathVariable Long id,
            @Valid @RequestBody CancelRepairRequestRequest req,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        RepairRequestResponse result = repairRequestUseCase.cancel(id, req.getReason(), user.getId());
        return ResponseEntity.ok(ApiResponse.success("Đã hủy yêu cầu sửa chữa", result));
    }

    @GetMapping("/matching")
    @PreAuthorize("hasRole('TECHNICIAN')")
    @Operation(summary = "Yêu cầu phù hợp với thợ (TECHNICIAN)")
    public ResponseEntity<ApiPageResponse<RepairRequestResponse>> getMatching(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortOrder
    ) {
        Page<RepairRequestResponse> pageResult = repairRequestUseCase.getMatchingForTechnician(
                user.getId(), page, limit, search, sortBy, sortOrder);

        ApiPageResponse<RepairRequestResponse> response = ApiPageResponse.of(
                "Lấy danh sách yêu cầu phù hợp thành công",
                page,
                limit,
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                pageResult.getContent()
        );
        return ResponseEntity.ok(response);
    }
}
