package com.fixlink.adapter.in.web.controller;

import com.fixlink.adapter.in.web.dto.request.UpdateCustomerProfileRequest;
import com.fixlink.adapter.in.web.dto.response.ApiResponse;
import com.fixlink.adapter.in.web.dto.response.CustomerProfileResponseDto;
import com.fixlink.application.port.in.CustomerProfileUseCase;
import com.fixlink.application.port.in.CustomerProfileUseCase.AuditLogEntry;
import com.fixlink.application.port.in.CustomerProfileUseCase.Requester;
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
@Tag(name = "Customer Management", description = "Các API quản lý và cập nhật hồ sơ khách hàng")
public class CustomerController {

    private final CustomerProfileUseCase customerProfileUseCase;

    public CustomerController(CustomerProfileUseCase customerProfileUseCase) {
        this.customerProfileUseCase = customerProfileUseCase;
    }

    /** Nguời gọi luôn dựng từ token đã xác thực. */
    private Requester requesterOf(UserPrincipal principal) {
        return new Requester(principal.getId(), principal.getRole());
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
    public ResponseEntity<ApiResponse<CustomerProfileResponseDto>> getProfile(
            @PathVariable String userId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long id = parseUserId(userId);
        CustomerProfile profile = customerProfileUseCase.getProfile(id, requesterOf(principal));
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin hồ sơ thành công",
                CustomerProfileResponseDto.fromDomain(profile)));
    }

    @PutMapping("/{userId}/profile")
    @Operation(summary = "Cập nhật hồ sơ khách hàng", description = "Cập nhật thông tin hồ sơ cá nhân. Chặn sửa hồ sơ của người khác và ghi lại thay đổi vào nhật ký kiểm toán.")
    public ResponseEntity<ApiResponse<CustomerProfileResponseDto>> updateProfile(
            @PathVariable String userId,
            @Valid @RequestBody UpdateCustomerProfileRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long targetId = parseUserId(userId);
        // Người gọi luôn lấy từ token đã xác thực. Trước đây có nhánh dự phòng đọc
        // header X-User-Id do client tự khai, và khi thiếu cả hai thì mặc định coi
        // người gọi chính là chủ hồ sơ — tức là vô hiệu hoá luôn kiểm tra IDOR.
        Long currentUserId = principal.getId();

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
    @Operation(summary = "Xem nhật ký kiểm toán hồ sơ khách hàng", description = "Tra cứu lịch sử thay đổi hồ sơ khách hàng (Audit Trail)")
    public ResponseEntity<ApiResponse<List<AuditLogEntry>>> getAuditTrail(
            @PathVariable String userId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long id = parseUserId(userId);
        List<AuditLogEntry> auditTrail = customerProfileUseCase.getAuditTrail(id, requesterOf(principal));
        return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử kiểm toán thành công", auditTrail));
    }
}
