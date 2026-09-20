package com.fixlink.adapter.in.web.controller;

import com.fixlink.adapter.in.web.dto.request.*;
import com.fixlink.adapter.in.web.dto.response.*;
import com.fixlink.application.port.in.*;
import com.fixlink.application.port.in.AuthUseCase.AuthResult;
import com.fixlink.application.port.in.AuthUseCase.CustomerRegisterResult;
import com.fixlink.application.port.in.AuthUseCase.LoginCommand;
import com.fixlink.application.port.in.AuthUseCase.RegisterCustomerCommand;
import com.fixlink.application.port.in.AuthUseCase.RegisterTechnicianCommand;
import com.fixlink.application.port.in.AuthUseCase.TechnicianRegisterResult;
import com.fixlink.domain.model.User;
import com.fixlink.infrastructure.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Các API xác thực, đăng nhập, đăng ký và bảo mật tài khoản")
public class AuthController {

    private final AuthUseCase authUseCase;
    private final UserManagementUseCase userManagementUseCase;
    private final ChangePasswordUseCase changePasswordUseCase;
    private final PasswordResetUseCase passwordResetUseCase;
    private final SessionUseCase sessionUseCase;

    public AuthController(
            AuthUseCase authUseCase,
            UserManagementUseCase userManagementUseCase,
            ChangePasswordUseCase changePasswordUseCase,
            PasswordResetUseCase passwordResetUseCase,
            SessionUseCase sessionUseCase
    ) {
        this.authUseCase = authUseCase;
        this.userManagementUseCase = userManagementUseCase;
        this.changePasswordUseCase = changePasswordUseCase;
        this.passwordResetUseCase = passwordResetUseCase;
        this.sessionUseCase = sessionUseCase;
    }

    @GetMapping("/me")
    @Operation(summary = "Lấy thông tin tài khoản hiện tại", description = "Lấy thông tin người dùng đang đăng nhập qua JWT Token.")
    public ResponseEntity<ApiResponse<UserResponseDto>> getCurrentUser(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User user = userManagementUseCase.getUserById(principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin người dùng thành công",
                UserResponseDto.fromDomain(user)));
    }

    @PostMapping("/login")
    @Operation(summary = "Đăng nhập hệ thống", description = "Đăng nhập cho Khách hàng, Thợ và Admin. Trả về JWT Token.")
    public ResponseEntity<ApiResponse<AuthResult>> login(@Valid @RequestBody LoginRequest request) {
        LoginCommand command = new LoginCommand(
                request.getUsername(),
                request.getPassword(),
                request.getDeviceToken()
        );

        AuthResult result = authUseCase.login(command);
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", result));
    }

    @PostMapping("/register/customer")
    @Operation(summary = "Đăng ký tài khoản Khách hàng", description = "Tạo tài khoản mới cho Khách hàng và khởi tạo Customer Profile.")
    public ResponseEntity<ApiResponse<CustomerRegisterResult>> registerCustomer(@Valid @RequestBody RegisterCustomerRequest request) {
        RegisterCustomerCommand command = new RegisterCustomerCommand(
                request.getUsername(),
                request.getPassword(),
                request.getFullName(),
                request.getPhone(),
                request.getEmail()
        );

        CustomerRegisterResult result = authUseCase.registerCustomer(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Đăng ký tài khoản thành công", result));
    }

    @PostMapping("/register/technician")
    @Operation(summary = "Đăng ký tài khoản Thợ / Kỹ thuật viên", description = "Đăng ký tài khoản Thợ kèm thông tin hồ sơ & CCCD để Admin duyệt.")
    public ResponseEntity<ApiResponse<TechnicianRegisterResult>> registerTechnician(@Valid @RequestBody RegisterTechnicianRequest request) {
        RegisterTechnicianCommand command = new RegisterTechnicianCommand(
                request.getUsername(),
                request.getPassword(),
                request.getFullName(),
                request.getPhone(),
                request.getEmail(),
                request.getCitizenId(),
                request.getIdCardFrontUrl(),
                request.getIdCardBackUrl(),
                request.getBio(),
                request.getYearsExperience()
        );

        TechnicianRegisterResult result = authUseCase.registerTechnician(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Đăng ký thành công. Hồ sơ đang chờ Admin xác minh", result));
    }

    // =========================================================================
    // RC-13: Change Password
    // =========================================================================
    @PutMapping("/change-password")
    @Operation(summary = "Đổi mật khẩu tài khoản", description = "Đổi mật khẩu cho người dùng hiện tại và thu hồi các phiên đăng nhập khác.")
    public ResponseEntity<ApiResponse<ChangePasswordResponse>> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        ChangePasswordResponse response = changePasswordUseCase.changePassword(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(
                "Đổi mật khẩu thành công. Các phiên đăng nhập khác đã bị đăng xuất", response
        ));
    }

    // =========================================================================
    // RC-14: Forgot & Reset Password
    // =========================================================================
    @PostMapping("/forgot-password")
    @Operation(summary = "Yêu cầu đặt lại mật khẩu", description = "Gửi email chứa liên kết đặt lại mật khẩu (chống User Enumeration, rate limit 3 lần/giờ).")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        ApiResponse<Void> response = passwordResetUseCase.forgotPassword(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Đặt lại mật khẩu mới", description = "Xác nhận token và đặt mật khẩu mới với mã hóa BCrypt cost 12.")
    public ResponseEntity<ApiResponse<ResetPasswordResponse>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        ResetPasswordResponse response = passwordResetUseCase.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(
                "Đặt lại mật khẩu thành công. Vui lòng đăng nhập bằng mật khẩu mới", response
        ));
    }

    // =========================================================================
    // RC-15: Logout & Session Lifecycle
    // =========================================================================
    @PostMapping("/logout")
    @Operation(summary = "Đăng xuất tài khoản", description = "Thu hồi token JWT (blacklist) và hủy refresh token.")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody(required = false) LogoutRequest request
    ) {
        Long userId = principal != null ? principal.getId() : null;
        ApiResponse<Void> response = sessionUseCase.logout(userId, authHeader, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Làm mới Access Token", description = "Cơ chế Token Rotation: thu hồi refresh token cũ và cấp mới cặp access + refresh token.")
    public ResponseEntity<ApiResponse<AuthResult>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResult result = sessionUseCase.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Làm mới token thành công", result));
    }
}
