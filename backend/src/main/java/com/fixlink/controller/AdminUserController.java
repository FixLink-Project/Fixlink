package com.fixlink.controller;

import com.fixlink.dto.response.*;
import com.fixlink.dto.request.UpdateUserStatusRequest;
import com.fixlink.dto.request.VerifyTechnicianRequest;
import com.fixlink.entity.User;
import com.fixlink.security.CustomUserDetailsService;
import com.fixlink.service.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final CustomUserDetailsService userDetailsService;

    public AdminUserController(AdminUserService adminUserService,
                                CustomUserDetailsService userDetailsService) {
        this.adminUserService = adminUserService;
        this.userDetailsService = userDetailsService;
    }

    /**
     * RC-19: Admin View & Search User List
     * GET /api/v1/admin/users?page=1&limit=10&search=Trần&role=TECHNICIAN&status=PENDING&sortBy=createdAt&sortOrder=DESC
     */
    @GetMapping("/users")
    public ResponseEntity<PaginatedResponse<UserDto>> getUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "ALL") String role,
            @RequestParam(required = false, defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortOrder) {

        PaginatedResponse<UserDto> response = adminUserService.getUsers(
                page, limit, search, role, status, sortBy, sortOrder);

        return ResponseEntity.ok(response);
    }

    /**
     * RC-20: Admin View User Detail
     * GET /api/v1/admin/users/{id}
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(@PathVariable Long id) {
        ApiResponse<UserDto> response = adminUserService.getUserById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * RC-21: Admin Block / Unblock User
     * PATCH /api/v1/admin/users/{id}/status
     */
    @PatchMapping("/users/{id}/status")
    public ResponseEntity<ApiResponse<StatusUpdateResponse>> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusRequest request,
            Authentication authentication) {

        Long adminId = getAdminId(authentication);
        ApiResponse<StatusUpdateResponse> response = adminUserService.updateUserStatus(
                id, request.getStatus(), request.getReason(), adminId);

        return ResponseEntity.ok(response);
    }

    /**
     * RC-22: Admin Verify Technician Profile (KYC)
     * PATCH /api/v1/admin/technicians/{userId}/verify
     */
    @RequestMapping(
            value = {"/technicians/{userId}/verify", "/users/{userId}/verify"},
            method = {RequestMethod.PATCH, RequestMethod.POST}
    )
    public ResponseEntity<ApiResponse<StatusUpdateResponse>> verifyTechnician(
            @PathVariable Long userId,
            @Valid @RequestBody VerifyTechnicianRequest request,
            Authentication authentication) {

        Long adminId = getAdminId(authentication);
        ApiResponse<StatusUpdateResponse> response = adminUserService.verifyTechnician(
                userId, request.getVerificationStatus(),
                request.getNote(), request.getRejectionReason(), adminId);

        return ResponseEntity.ok(response);
    }

    /**
     * Extract admin user ID from the authenticated principal.
     */
    private Long getAdminId(Authentication authentication) {
        String username = authentication.getName();
        User adminUser = userDetailsService.getUserEntity(username);
        return adminUser.getId();
    }
}
