package com.fixlink.service;

import com.fixlink.dto.request.LoginRequest;
import com.fixlink.dto.response.ApiResponse;
import com.fixlink.dto.response.LoginResponse;
import com.fixlink.entity.TechnicianProfile;
import com.fixlink.entity.User;
import com.fixlink.enums.UserRole;
import com.fixlink.enums.UserStatus;
import com.fixlink.enums.VerificationStatus;
import com.fixlink.exception.InvalidOperationException;
import com.fixlink.exception.TooManyRequestsException;
import com.fixlink.repository.UserRepository;
import com.fixlink.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final LoginAttemptService loginAttemptService;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtTokenProvider tokenProvider,
                       UserRepository userRepository,
                       LoginAttemptService loginAttemptService) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
        this.userRepository = userRepository;
        this.loginAttemptService = loginAttemptService;
    }

    public ApiResponse<LoginResponse> login(LoginRequest request) {
        String username = request.getUsername();

        // ── AC 4: Check if account is temporarily locked due to too many failed attempts ──
        if (loginAttemptService.isLocked(username)) {
            long remainingSeconds = loginAttemptService.getRemainingLockSeconds(username);
            throw new TooManyRequestsException(
                    "Tài khoản tạm thời bị khóa do nhập sai quá nhiều lần. Vui lòng thử lại sau.",
                    remainingSeconds);
        }

        // Check if user exists (generic error to prevent username enumeration)
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    loginAttemptService.recordFailure(username);
                    return new BadCredentialsException(
                            "Tên đăng nhập hoặc mật khẩu không chính xác");
                });

        // ── AC 3: Check if account is blocked ──
        if (user.getStatus() == UserStatus.BANNED) {
            throw new InvalidOperationException(
                    "Tài khoản của bạn đã bị khóa do vi phạm chính sách",
                    "ACCOUNT_BLOCKED");
        }

        // ── AC 3: Check if account is inactive ──
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new InvalidOperationException(
                    "Tài khoản của bạn chưa được kích hoạt hoặc đã bị vô hiệu hóa",
                    "ACCOUNT_INACTIVE");
        }

        // ── AC 1 & AC 2: Authenticate credentials ──
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.getPassword()));
        } catch (BadCredentialsException ex) {
            // AC 4: Record failed attempt
            loginAttemptService.recordFailure(username);

            // Check if this failure triggered a lock
            if (loginAttemptService.isLocked(username)) {
                long remainingSeconds = loginAttemptService.getRemainingLockSeconds(username);
                throw new TooManyRequestsException(
                        "Tài khoản tạm thời bị khóa do nhập sai quá nhiều lần. Vui lòng thử lại sau.",
                        remainingSeconds);
            }
            throw new BadCredentialsException("Tên đăng nhập hoặc mật khẩu không chính xác");
        }

        // AC 4: Reset attempt counter on successful login
        loginAttemptService.recordSuccess(username);

        // Generate JWT token
        String accessToken = tokenProvider.generateToken(
                authentication, user.getId(), user.getRole().name());

        // Build user info
        String fullName = getFullName(user);
        String avatarUrl = getAvatarUrl(user);

        // ── AC 5: Determine verification status for Technician ──
        boolean isVerified = true;
        String verificationStatus = null;

        if (user.getRole() == UserRole.TECHNICIAN && user.getTechnicianProfile() != null) {
            TechnicianProfile techProfile = user.getTechnicianProfile();
            VerificationStatus vs = techProfile.getVerificationStatus();
            verificationStatus = (vs != null) ? vs.name() : VerificationStatus.PENDING.name();
            isVerified = (vs == VerificationStatus.APPROVED);
        }

        LoginResponse loginResponse = LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(UUID.randomUUID().toString())
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getExpirationMs() / 1000)
                .user(LoginResponse.UserInfo.builder()
                        .id("usr_" + user.getId())
                        .username(user.getUsername())
                        .role(user.getRole().name())
                        .status(user.getStatus().name())
                        .fullName(fullName)
                        .avatarUrl(avatarUrl)
                        .isVerified(isVerified)
                        .verificationStatus(verificationStatus)
                        .build())
                .build();

        return ApiResponse.success("Đăng nhập thành công", loginResponse);
    }

    private String getFullName(User user) {
        if (user.getCustomerProfile() != null) {
            return user.getCustomerProfile().getName();
        }
        if (user.getTechnicianProfile() != null) {
            return user.getTechnicianProfile().getName();
        }
        return user.getUsername();
    }

    private String getAvatarUrl(User user) {
        if (user.getCustomerProfile() != null) {
            return user.getCustomerProfile().getAvatarUrl();
        }
        if (user.getTechnicianProfile() != null) {
            return user.getTechnicianProfile().getAvatarUrl();
        }
        return null;
    }
}
