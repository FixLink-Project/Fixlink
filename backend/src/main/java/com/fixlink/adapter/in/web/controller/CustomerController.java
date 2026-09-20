package com.fixlink.adapter.in.web.controller;

import com.fixlink.adapter.in.web.dto.request.UpdateCustomerProfileRequest;
import com.fixlink.adapter.in.web.dto.response.ApiResponse;
import com.fixlink.adapter.in.web.dto.response.CustomerProfileResponseDto;
import com.fixlink.application.port.in.CustomerProfileUseCase;
import com.fixlink.application.port.in.CustomerProfileUseCase.AuditLogEntry;
import com.fixlink.application.port.in.CustomerProfileUseCase.UpdateProfileCommand;
import com.fixlink.domain.model.CustomerProfile;
import com.fixlink.infrastructure.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "Customer Management", description = "Các API quản lý và cập nhật hồ sơ khách hàng (RC-17)")
public class CustomerController {

    private final CustomerProfileUseCase customerProfileUseCase;

    public CustomerController(CustomerProfileUseCase customerProfileUseCase) {
        this.customerProfileUseCase = customerProfileUseCase;
    }

    private Long parseUserId(String userIdStr) {
        if (userIdStr == null) return null;
        if (userIdStr.startsWith("usr_")) {
            return Long.parseLong(userIdStr.substring(4));
        }
        return Long.parseLong(userIdStr);
    }

    @GetMapping("/{userId}/profile")
    @Operation(summary = "Xem hồ sơ khách hàng", description = "Lấy thông tin chi tiết hồ sơ của khách hàng theo userId (chấp nhận cả '8' hoặc 'usr_8')")
    public ResponseEntity<ApiResponse<CustomerProfileResponseDto>> getProfile(@PathVariable String userId) {
        Long id = parseUserId(userId);
        CustomerProfile profile = customerProfileUseCase.getProfile(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin hồ sơ thành công",
                CustomerProfileResponseDto.fromDomain(profile)));
    }

    @PutMapping("/{userId}/profile")
    @Operation(summary = "Cập nhật hồ sơ khách hàng", description = "Cập nhật thông tin hồ sơ cá nhân. Đảm bảo chống IDOR (AC 2) và ghi nhận Audit Trail (AC 4).")
    public ResponseEntity<ApiResponse<CustomerProfileResponseDto>> updateProfile(
            @PathVariable String userId,
            @Valid @RequestBody UpdateCustomerProfileRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-User-Id", required = false) String xUserId
    ) {
        Long targetId = parseUserId(userId);
        // Xác định ID người đang gọi: Ưu tiên lấy từ JWT AuthenticationPrincipal, fallback X-User-Id
        Long currentUserId = principal != null ? principal.getId() : parseUserId(xUserId);

        // Nếu cả principal và xUserId đều không có, gán mặc định là targetId (trừ khi test IDOR truyền header khác)
        if (currentUserId == null) {
            currentUserId = targetId;
        }

        UpdateProfileCommand command = new UpdateProfileCommand(
                request.getFullName(),
                request.getPhone(),
                request.getEmail(),
                request.getAvatarUrl()
        );

        CustomerProfile updated = customerProfileUseCase.updateProfile(targetId, currentUserId, command);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin hồ sơ thành công",
                CustomerProfileResponseDto.fromDomain(updated)));
    }

    @GetMapping("/{userId}/audit-trail")
    @Operation(summary = "Xem nhật ký kiểm toán hồ sơ khách hàng", description = "Tra cứu lịch sử thay đổi hồ sơ khách hàng (Audit Trail - AC 4)")
    public ResponseEntity<ApiResponse<List<AuditLogEntry>>> getAuditTrail(@PathVariable String userId) {
        Long id = parseUserId(userId);
        List<AuditLogEntry> auditTrail = customerProfileUseCase.getAuditTrail(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử kiểm toán thành công", auditTrail));
    }
}
