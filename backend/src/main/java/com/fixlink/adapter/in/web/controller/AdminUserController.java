package com.fixlink.adapter.in.web.controller;

import com.fixlink.adapter.in.web.dto.request.VerifyTechnicianRequest;
import com.fixlink.adapter.in.web.dto.response.ApiPageResponse;
import com.fixlink.adapter.in.web.dto.response.ApiResponse;
import com.fixlink.adapter.in.web.dto.response.UserResponseDto;
import com.fixlink.application.port.in.TechnicianVerificationUseCase;
import com.fixlink.application.port.in.TechnicianVerificationUseCase.VerificationResult;
import com.fixlink.application.port.in.TechnicianVerificationUseCase.VerifyTechnicianCommand;
import com.fixlink.application.port.in.UserManagementUseCase;
import com.fixlink.application.port.in.UserManagementUseCase.UserPageResult;
import com.fixlink.application.port.in.UserManagementUseCase.UserQuery;
import com.fixlink.domain.model.User;
import com.fixlink.infrastructure.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin Management", description = "Các API quản trị hệ thống dành cho Admin")
@SecurityRequirement(name = "bearerAuth")
public class AdminUserController {

    private final UserManagementUseCase userManagementUseCase;
    private final TechnicianVerificationUseCase technicianVerificationUseCase;
    private final com.fixlink.application.port.in.TechnicianProfileUseCase technicianProfileUseCase;

    public AdminUserController(UserManagementUseCase userManagementUseCase,
                               TechnicianVerificationUseCase technicianVerificationUseCase,
                               com.fixlink.application.port.in.TechnicianProfileUseCase technicianProfileUseCase) {
        this.userManagementUseCase = userManagementUseCase;
        this.technicianVerificationUseCase = technicianVerificationUseCase;
        this.technicianProfileUseCase = technicianProfileUseCase;
    }

    @GetMapping("/users")
    @Operation(summary = "Xem danh sách người dùng", description = "Admin xem danh sách người dùng có tìm kiếm, lọc theo vai trò, trạng thái, phân trang và sắp xếp.")
    public ResponseEntity<ApiPageResponse<UserResponseDto>> getUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "ALL") String role,
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortOrder
    ) {
        UserQuery query = new UserQuery(page, limit, search, role, status, sortBy, sortOrder);
        UserPageResult pageResult = userManagementUseCase.getUsers(query);

        List<UserResponseDto> dtoList = pageResult.users().stream()
                .map(UserResponseDto::fromDomain)
                .toList();

        ApiPageResponse<UserResponseDto> response = ApiPageResponse.of(
                "Lấy danh sách người dùng thành công",
                pageResult.currentPage(),
                pageResult.limit(),
                pageResult.totalItems(),
                pageResult.totalPages(),
                dtoList
        );

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/technicians/{userId}/verify")
    @Operation(summary = "Duyệt hoặc từ chối hồ sơ Thợ", description = "Admin phê duyệt hoặc từ chối xác minh hồ sơ Kỹ thuật viên kèm lý do.")
    public ResponseEntity<ApiResponse<VerificationResult>> verifyTechnician(
            @PathVariable String userId,
            @Valid @RequestBody VerifyTechnicianRequest request,
            @AuthenticationPrincipal UserPrincipal adminUser
    ) {
        // Strip "usr_" prefix if provided
        Long parsedUserId = parseUserId(userId);
        Long adminId = (adminUser != null) ? adminUser.getId() : 1L;

        VerifyTechnicianCommand command = new VerifyTechnicianCommand(
                parsedUserId,
                request.getVerificationStatus(),
                request.getNote(),
                request.getRejectionReason(),
                adminId
        );

        VerificationResult result = technicianVerificationUseCase.verifyTechnician(command);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái xác minh thợ thành công", result));
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "Xem chi tiết người dùng", description = "Admin xem chi tiết hồ sơ người dùng theo ID.")
    public ResponseEntity<ApiResponse<UserResponseDto>> getUserDetail(@PathVariable String userId) {
        Long parsedUserId = parseUserId(userId);
        User user = userManagementUseCase.getUserById(parsedUserId);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin chi tiết người dùng thành công", UserResponseDto.fromDomain(user)));
    }

    @PatchMapping("/users/{userId}/status")
    @Operation(summary = "Khóa hoặc Mở khóa người dùng", description = "Admin cập nhật trạng thái người dùng (ACTIVE / BLOCKED).")
    public ResponseEntity<ApiResponse<Void>> updateUserStatus(
            @PathVariable String userId,
            @RequestBody java.util.Map<String, String> body
    ) {
        Long parsedUserId = parseUserId(userId);
        String statusStr = body.get("status");
        com.fixlink.domain.model.UserStatus status = com.fixlink.domain.model.UserStatus.valueOf(statusStr.toUpperCase());
        userManagementUseCase.updateUserStatus(parsedUserId, status);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái người dùng thành công", null));
    }

    @PutMapping("/users/{userId}/approve")
    @Operation(summary = "Phê duyệt tài khoản Thợ", description = "Admin duyệt tài khoản Thợ đang chờ duyệt: PENDING_APPROVAL -> ACTIVE.")
    public ResponseEntity<ApiResponse<UserResponseDto>> approveUser(@PathVariable String userId) {
        Long parsedUserId = parseUserId(userId);
        User user = userManagementUseCase.approveUser(parsedUserId);
        return ResponseEntity.ok(ApiResponse.success("Phê duyệt tài khoản thành công", UserResponseDto.fromDomain(user)));
    }

    @PutMapping("/users/{userId}/reject")
    @Operation(summary = "Từ chối tài khoản Thợ", description = "Admin từ chối tài khoản Thợ: PENDING_APPROVAL -> REJECTED.")
    public ResponseEntity<ApiResponse<UserResponseDto>> rejectUser(@PathVariable String userId) {
        Long parsedUserId = parseUserId(userId);
        User user = userManagementUseCase.rejectUser(parsedUserId);
        return ResponseEntity.ok(ApiResponse.success("Từ chối tài khoản thành công", UserResponseDto.fromDomain(user)));
    }

    @PutMapping("/technicians/{userId}/categories")
    @Operation(summary = "Admin cập nhật chuyên môn thợ", description = "Admin sau khi phỏng vấn/thẩm định tay nghề thực tế sẽ cập nhật danh mục được cấp phép cho thợ.")
    public ResponseEntity<ApiResponse<UserResponseDto>> updateTechnicianCategories(
            @PathVariable String userId,
            @RequestBody java.util.Map<String, List<Long>> body
    ) {
        Long parsedUserId = parseUserId(userId);
        List<Long> categoryIds = body.getOrDefault("categoryIds", List.of());
        technicianProfileUseCase.updateCategoriesByAdmin(parsedUserId, categoryIds);
        User user = userManagementUseCase.getUserById(parsedUserId);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật chuyên môn kỹ thuật viên thành công", UserResponseDto.fromDomain(user)));
    }

    private Long parseUserId(String userIdStr) {
        if (userIdStr.startsWith("usr_")) {
            return Long.parseLong(userIdStr.substring(4));
        }
        return Long.parseLong(userIdStr);
    }
}
