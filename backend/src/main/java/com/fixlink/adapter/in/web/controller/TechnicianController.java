package com.fixlink.adapter.in.web.controller;

import com.fixlink.adapter.in.web.dto.response.ApiResponse;
import com.fixlink.application.port.in.TechnicianUseCase;
import com.fixlink.domain.model.TechnicianProfile;
import com.fixlink.infrastructure.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/technicians")
@Tag(name = "Technician", description = "Các API dành cho Kỹ thuật viên / Thợ")
@SecurityRequirement(name = "bearerAuth")
public class TechnicianController {

    private final TechnicianUseCase technicianUseCase;
    private final com.fixlink.application.port.in.TechnicianProfileUseCase technicianProfileUseCase;

    public TechnicianController(TechnicianUseCase technicianUseCase,
                                com.fixlink.application.port.in.TechnicianProfileUseCase technicianProfileUseCase) {
        this.technicianUseCase = technicianUseCase;
        this.technicianProfileUseCase = technicianProfileUseCase;
    }

    @GetMapping("/me/profile")
    @Operation(summary = "Xem thông tin hồ sơ thợ", description = "Lấy thông tin cá nhân, tay nghề và trạng thái xác minh KYC của thợ đang đăng nhập.")
    public ResponseEntity<ApiResponse<com.fixlink.adapter.in.web.dto.response.TechnicianProfileResponse>> getMyProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long userId
    ) {
        Long targetUserId = (principal != null) ? principal.getId() : (userId != null ? userId : 3L);
        com.fixlink.adapter.in.web.dto.response.TechnicianProfileResponse profile = technicianProfileUseCase.getMyProfile(targetUserId);
        return ResponseEntity.ok(ApiResponse.success("Lấy hồ sơ kỹ thuật viên thành công", profile));
    }

    @PutMapping("/me/profile")
    @Operation(summary = "Cập nhật thông tin hồ sơ thợ", description = "Kỹ thuật viên cập nhật thông tin cá nhân, tay nghề, năm kinh nghiệm, tiểu sử (Technician Profile Self-Service).")
    public ResponseEntity<ApiResponse<com.fixlink.adapter.in.web.dto.response.TechnicianProfileResponse>> updateMyProfile(
            @jakarta.validation.Valid @RequestBody com.fixlink.adapter.in.web.dto.request.UpdateTechnicianProfileRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long userId
    ) {
        Long targetUserId = (principal != null) ? principal.getId() : (userId != null ? userId : 3L);
        com.fixlink.adapter.in.web.dto.response.TechnicianProfileResponse updated = technicianProfileUseCase.updateMyProfile(targetUserId, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật hồ sơ kỹ thuật viên thành công", updated));
    }

    @PatchMapping("/me/status/online")
    @Operation(summary = "Bật/Tắt trạng thái nhận việc", description = "Bật hoặc tắt chế độ sẵn sàng nhận yêu cầu sửa chữa. Bị từ chối (403) nếu thợ chưa được duyệt KYC.")
    public ResponseEntity<ApiResponse<TechnicianProfile>> updateOnlineStatus(
            @RequestBody Map<String, Boolean> body,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long userId
    ) {
        Long targetUserId = (principal != null) ? principal.getId() : (userId != null ? userId : 3L);
        boolean isOnline = body != null && Boolean.TRUE.equals(body.get("isOnline"));
        TechnicianProfile updated = technicianUseCase.updateOnlineStatus(targetUserId, isOnline);
        return ResponseEntity.ok(ApiResponse.success(
                isOnline ? "Đã bật chế độ sẵn sàng nhận việc" : "Đã tạm dừng nhận việc",
                updated
        ));
    }

    @GetMapping("/me/repair-requests")
    @Operation(summary = "Xem danh sách yêu cầu sửa chữa", description = "Lấy các yêu cầu sửa chữa gửi tới thợ. Bị từ chối (403) nếu thợ chưa được duyệt KYC.")
    public ResponseEntity<ApiResponse<List<TechnicianUseCase.RepairRequestDto>>> getMyRepairRequests(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long userId
    ) {
        Long targetUserId = (principal != null) ? principal.getId() : (userId != null ? userId : 3L);
        List<TechnicianUseCase.RepairRequestDto> requests = technicianUseCase.getMyRepairRequests(targetUserId);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách yêu cầu sửa chữa thành công", requests));
    }
}
