package com.fixlink.adapter.in.web.controller;

import com.fixlink.adapter.in.web.dto.request.AttachMediaRequest;
import com.fixlink.adapter.in.web.dto.request.CancelRepairRequestRequest;
import com.fixlink.adapter.in.web.dto.request.CreateRepairRequestRequest;
import com.fixlink.adapter.in.web.dto.request.UpdateRepairRequestRequest;
import com.fixlink.adapter.in.web.dto.request.UpdateRepairRequestStatusRequest;
import com.fixlink.adapter.in.web.dto.response.*;
import com.fixlink.application.port.in.RepairRequestUseCase;
import com.fixlink.application.port.in.RepairRequestUseCase.CreateRepairRequestCommand;
import com.fixlink.application.port.in.RepairRequestUseCase.RepairRequestDetail;
import com.fixlink.application.port.in.RepairRequestUseCase.RepairRequestPageResult;
import com.fixlink.application.port.in.RepairRequestUseCase.RepairRequestQuery;
import com.fixlink.domain.exception.ValidationFailedException;
import com.fixlink.domain.model.RequestStatus;
import com.fixlink.domain.model.RequestTab;
import com.fixlink.infrastructure.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Controller quản lý Yêu cầu Sửa chữa:
 * - RC-30: Danh sách chia 5 tab trạng thái + phân trang đánh số, gắn ảnh bằng chứng, cập nhật trạng thái.
 * - RC-8: Tạo mới kèm ảnh hiện trường Firebase Storage, lưu bản nháp.
 * - Sprint 2: Đấu thầu báo giá, danh sách phù hợp thợ, chi tiết báo giá và tiến độ.
 */
@RestController
@RequestMapping("/api/v1/repair-requests")
@RequiredArgsConstructor
@Tag(name = "Repair Requests", description = "Quản lý yêu cầu sửa chữa: tab trạng thái, phân trang đánh số, ảnh Firebase Storage, báo giá")
@SecurityRequirement(name = "bearerAuth")
public class RepairRequestController {

    private final RepairRequestUseCase repairRequestUseCase;

    // ==================================================================================
    // 1. RC-30 & RC-8 ENDPOINTS
    // ==================================================================================

    @GetMapping
    @Operation(summary = "Danh sách yêu cầu sửa chữa (chia tab + phân trang đánh số)",
            description = "Trả về block meta (currentPage, limit, totalItems, totalPages, hasNext, hasPrevious) để render "
                    + "thanh phân trang đánh số. Tham số tab: ALL | AWAITING_QUOTE | IN_PROGRESS | COMPLETED | CANCELLED. "
                    + "Khách hàng chỉ thấy yêu cầu của mình, Kỹ thuật viên thấy việc được giao, Admin thấy toàn bộ.")
    public ResponseEntity<ApiPageResponse<RepairRequestResponse>> getRepairRequests(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam(defaultValue = "ALL") String tab,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortOrder,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long viewerId = requireUserId(principal);
        RepairRequestQuery query = new RepairRequestQuery(
                page, limit, tab, search, sortBy, sortOrder, viewerId, principal.getRole());

        RepairRequestPageResult pageResult = repairRequestUseCase.listRequests(query);
        List<RepairRequestResponse> data = pageResult.items().stream()
                .map(RepairRequestResponse::fromDetail)
                .toList();

        return ResponseEntity.ok(ApiPageResponse.of(
                "Lấy danh sách yêu cầu sửa chữa thành công",
                pageResult.currentPage(),
                pageResult.limit(),
                pageResult.totalItems(),
                pageResult.totalPages(),
                data
        ));
    }

    @GetMapping("/tabs")
    @Operation(summary = "Danh sách tab trạng thái",
            description = "Trả về 5 tab nghiệp vụ (kèm nhãn tiếng Việt) dùng để render thanh chia tab trên giao diện.")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTabs() {
        List<Map<String, Object>> tabs = RequestTab.uiTabs().stream()
                .map(item -> Map.<String, Object>of(
                        "code", item.name(),
                        "label", item.getLabel(),
                        "statuses", item.getStatuses().stream().map(Enum::name).toList()
                ))
                .toList();

        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách tab trạng thái thành công", tabs));
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Tạo yêu cầu sửa chữa mới",
            description = "Khách hàng đăng yêu cầu kèm danh sách URL ảnh hiện trường đã upload lên Firebase Storage "
                    + "(mediaUrls). Hỗ trợ lưu nháp với saveAsDraft=true.")
    public ResponseEntity<ApiResponse<RepairRequestResponse>> create(
            @Valid @RequestBody CreateRepairRequestRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long customerId = requireUserId(principal);

        RepairRequestDetail detail = repairRequestUseCase.createRequest(new CreateRepairRequestCommand(
                customerId,
                request.getTitle(),
                request.getDescription(),
                request.getAddress(),
                request.getCategoryId(),
                request.getServiceId(),
                request.getAreaId(),
                request.getLatitude(),
                request.getLongitude(),
                request.getBudgetRef(),
                request.getBiddingDeadlineDays(),
                request.getRequestedTime() != null ? request.getRequestedTime() : request.getPreferredTime(),
                Boolean.TRUE.equals(request.getSaveAsDraft()),
                request.getMediaUrls(),
                request.getDeviceBrand(),
                request.getDeviceModel(),
                request.getSerialNumber()
        ));

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(
                "Tạo yêu cầu sửa chữa thành công", RepairRequestResponse.fromDetail(detail)));
    }

    @PatchMapping("/{requestId}/status")
    @Operation(summary = "Cập nhật trạng thái yêu cầu sửa chữa",
            description = "Admin có thể chuyển sang mọi trạng thái; Khách hàng chỉ được HỦY yêu cầu của chính mình "
                    + "hoặc ĐĂNG bản nháp (DRAFT -> PENDING).")
    public ResponseEntity<ApiResponse<RepairRequestResponse>> updateRepairRequestStatus(
            @PathVariable String requestId,
            @Valid @RequestBody UpdateRepairRequestStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long actorId = requireUserId(principal);

        RepairRequestDetail detail = repairRequestUseCase.updateStatus(new RepairRequestUseCase.UpdateStatusCommand(
                parseRequestId(requestId),
                parseStatus(request.getStatus()),
                request.getNote(),
                actorId,
                principal.getRole()
        ));

        return ResponseEntity.ok(ApiResponse.success(
                "Cập nhật trạng thái yêu cầu sửa chữa thành công", RepairRequestResponse.fromDetail(detail)));
    }

    @PostMapping("/{requestId}/media")
    @Operation(summary = "Gắn ảnh bằng chứng vào yêu cầu sửa chữa",
            description = "Gắn thêm tệp (ảnh/video hiện trường đã upload lên Firebase Storage) vào yêu cầu. "
                    + "Tối đa 6 tệp trên mỗi yêu cầu.")
    public ResponseEntity<ApiResponse<RepairRequestResponse>> attachMedia(
            @PathVariable String requestId,
            @Valid @RequestBody AttachMediaRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long actorId = requireUserId(principal);

        RepairRequestDetail detail = repairRequestUseCase.attachMedia(new RepairRequestUseCase.AttachMediaCommand(
                parseRequestId(requestId),
                request.getMediaUrls(),
                actorId,
                principal.getRole()
        ));

        return ResponseEntity.ok(ApiResponse.success(
                "Gắn tệp bằng chứng vào yêu cầu sửa chữa thành công", RepairRequestResponse.fromDetail(detail)));
    }

    // ==================================================================================
    // 2. SPRINT 2 ENDPOINTS (Chi tiết có báo giá, sửa, hủy, thợ xem việc phù hợp)
    // ==================================================================================

    @GetMapping("/{id}")
    @Operation(summary = "Chi tiết yêu cầu sửa chữa (kèm báo giá và tiến độ công việc)")
    public ResponseEntity<ApiResponse<RepairRequestDetailResponse>> getDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        RepairRequestDetailResponse detail = repairRequestUseCase.getDetail(
                id, requireUserId(user), user.getRole().name());
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

    @GetMapping("/matching")
    @PreAuthorize("hasRole('TECHNICIAN')")
    @Operation(summary = "Yêu cầu phù hợp với thợ (TECHNICIAN)",
            description = "Tìm kiếm và lọc các yêu cầu sửa chữa đang mở thầu theo khu vực hoạt động (RC-38) và từ khóa.")
    public ResponseEntity<ApiPageResponse<RepairRequestResponse>> getMatching(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) Long areaId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortOrder
    ) {
        Page<RepairRequestResponse> pageResult = repairRequestUseCase.getMatchingForTechnician(
                user.getId(), areaId, page, limit, search, sortBy, sortOrder);

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

    // ==================================================================================
    // 3. HELPER METHODS
    // ==================================================================================

    private Long requireUserId(UserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new AccessDeniedException("Vui lòng đăng nhập để thực hiện thao tác này");
        }
        return principal.getId();
    }

    private Long parseRequestId(String requestId) {
        String normalized = requestId != null ? requestId.trim() : "";
        if (normalized.toLowerCase(Locale.ROOT).startsWith("req_")) {
            normalized = normalized.substring(4);
        }
        try {
            return Long.parseLong(normalized);
        } catch (NumberFormatException ex) {
            throw new ValidationFailedException("Mã yêu cầu không hợp lệ",
                    Map.of("requestId", "Mã yêu cầu phải là số"));
        }
    }

    private RequestStatus parseStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            throw new ValidationFailedException("Trạng thái không hợp lệ",
                    Map.of("status", "Trạng thái không được để trống"));
        }
        try {
            return RequestStatus.valueOf(rawStatus.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new ValidationFailedException("Trạng thái không hợp lệ",
                    Map.of("status", "Trạng thái không nằm trong danh sách cho phép"));
        }
    }
}
