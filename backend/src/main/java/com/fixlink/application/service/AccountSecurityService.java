package com.fixlink.application.service;

import com.fixlink.adapter.in.web.dto.request.ChangePasswordRequest;
import com.fixlink.adapter.in.web.dto.request.ForgotPasswordRequest;
import com.fixlink.adapter.in.web.dto.request.LogoutRequest;
import com.fixlink.adapter.in.web.dto.request.RefreshTokenRequest;
import com.fixlink.adapter.in.web.dto.request.ResetPasswordRequest;
import com.fixlink.adapter.in.web.dto.response.ApiResponse;
import com.fixlink.adapter.in.web.dto.response.ChangePasswordResponse;
import com.fixlink.adapter.in.web.dto.response.ResetPasswordResponse;
import com.fixlink.adapter.out.persistence.entity.CustomerProfileJpaEntity;
import com.fixlink.adapter.out.persistence.entity.PasswordResetTokenJpaEntity;
import com.fixlink.adapter.out.persistence.entity.RefreshTokenJpaEntity;
import com.fixlink.adapter.out.persistence.entity.TechnicianProfileJpaEntity;
import com.fixlink.adapter.out.persistence.entity.TokenBlacklistJpaEntity;
import com.fixlink.adapter.out.persistence.entity.UserJpaEntity;
import com.fixlink.adapter.out.persistence.repository.SpringDataCustomerProfileRepository;
import com.fixlink.adapter.out.persistence.repository.SpringDataPasswordResetTokenRepository;
import com.fixlink.adapter.out.persistence.repository.SpringDataRefreshTokenRepository;
import com.fixlink.adapter.out.persistence.repository.SpringDataTechnicianProfileRepository;
import com.fixlink.adapter.out.persistence.repository.SpringDataTokenBlacklistRepository;
import com.fixlink.adapter.out.persistence.repository.SpringDataUserRepository;
import com.fixlink.application.port.in.AuthUseCase;
import com.fixlink.application.port.in.ChangePasswordUseCase;
import com.fixlink.application.port.in.PasswordResetUseCase;
import com.fixlink.application.port.in.SessionUseCase;
import com.fixlink.domain.exception.*;
import com.fixlink.domain.model.Role;
import com.fixlink.domain.model.UserStatus;
import com.fixlink.domain.model.VerificationStatus;
import com.fixlink.domain.util.PasswordValidator;
import com.fixlink.infrastructure.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountSecurityService implements ChangePasswordUseCase, PasswordResetUseCase, SessionUseCase {

    /**
     * Một thông báo duy nhất cho mọi trường hợp: email có thật, không tồn tại, hay
     * vượt ngưỡng yêu cầu. Khác nhau một chữ là lộ email nào đã đăng ký.
     */
    private static final String NEUTRAL_FORGOT_PASSWORD_MESSAGE =
            "Nếu email đã được đăng ký trong hệ thống, một liên kết đặt lại mật khẩu đã được gửi đến hộp thư của bạn. Vui lòng kiểm tra email (bao gồm thư mục Spam) trong vòng 15 phút.";

    /** Băm token để cơ sở dữ liệu không bao giờ giữ bản dùng được. */
    private static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Máy chủ không hỗ trợ thuật toán SHA-256", e);
        }
    }

    private final SpringDataUserRepository userRepository;
    private final SpringDataCustomerProfileRepository customerProfileRepository;
    private final SpringDataTechnicianProfileRepository technicianProfileRepository;
    private final SpringDataPasswordResetTokenRepository passwordResetTokenRepository;
    private final SpringDataRefreshTokenRepository refreshTokenRepository;
    private final SpringDataTokenBlacklistRepository tokenBlacklistRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;

    // =========================================================================
    // Đổi mật khẩu
    // =========================================================================
    @Override
    @Transactional
    public ChangePasswordResponse changePassword(Long userId, ChangePasswordRequest request) {
        UserJpaEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        // 1. Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new InvalidCurrentPasswordException();
        }

        // 2. Verify confirmation matches
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            Map<String, String> errors = new HashMap<>();
            errors.put("confirmPassword", "Mật khẩu xác nhận không khớp với mật khẩu mới");
            throw new ValidationFailedException("Dữ liệu đầu vào không hợp lệ", errors);
        }

        // 3. Validate password policy (≥8 chars, 1 upper, 1 lower, 1 digit, 1 special)
        if (!PasswordValidator.isValid(request.getNewPassword())) {
            Map<String, String> errors = new HashMap<>();
            errors.put("newPassword", "Mật khẩu phải có tối thiểu 8 ký tự, bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt");
            throw new ValidationFailedException("Dữ liệu đầu vào không hợp lệ", errors);
        }

        // 4. Verify new password != current password
        if (request.getNewPassword().equals(request.getCurrentPassword())) {
            throw new PasswordSameAsOldException();
        }

        // 5. Update password with BCrypt cost 12
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // 6. Invalidate other refresh tokens
        refreshTokenRepository.deleteByUserId(userId);

        return ChangePasswordResponse.builder()
                .userId(user.getId())
                .passwordChangedAt(user.getUpdatedAt())
                .build();
    }

    // =========================================================================
    // Quên mật khẩu và đặt lại mật khẩu
    // =========================================================================
    @Override
    @Transactional
    public ApiResponse<Void> forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().trim();

        // Anti-enumeration: Check in customer/technician profiles or users table
        Optional<UserJpaEntity> userOpt = Optional.empty();
        Optional<CustomerProfileJpaEntity> custOpt = customerProfileRepository.findByEmail(email);
        if (custOpt.isPresent()) {
            userOpt = Optional.of(custOpt.get().getUser());
        } else {
            Optional<TechnicianProfileJpaEntity> techOpt = technicianProfileRepository.findByEmail(email);
            if (techOpt.isPresent()) {
                userOpt = Optional.of(techOpt.get().getUser());
            } else {
                userOpt = userRepository.findByUsernameAndDeletedAtIsNull(email);
            }
        }

        if (userOpt.isEmpty()) {
            log.info("Password reset requested for non-existing email: {}", email);
            return ApiResponse.success(
                    NEUTRAL_FORGOT_PASSWORD_MESSAGE,
                    null
            );
        }

        UserJpaEntity user = userOpt.get();

        // Giới hạn 3 yêu cầu mỗi giờ. Khi vượt ngưỡng vẫn trả đúng thông báo trung lập
        // như với email không tồn tại: nếu trả 429 riêng cho email có thật thì chỉ cần
        // gửi quá ngưỡng là dò được email nào đã đăng ký.
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long count = passwordResetTokenRepository.countByEmailInLastHour(email, oneHourAgo);
        if (count >= 3) {
            log.warn("Vượt ngưỡng yêu cầu đặt lại mật khẩu cho email: {}", email);
            return ApiResponse.success(NEUTRAL_FORGOT_PASSWORD_MESSAGE, null);
        }

        // Invalidate older unused tokens
        passwordResetTokenRepository.invalidateUnusedTokensByUserId(user.getId());

        // Token gửi cho người dùng là UUID ngẫu nhiên, cơ sở dữ liệu chỉ giữ bản băm
        // SHA-256 để người đọc được bảng cũng không dùng lại được liên kết.
        String token = UUID.randomUUID().toString();
        PasswordResetTokenJpaEntity resetToken = PasswordResetTokenJpaEntity.builder()
                .token(hashToken(token))
                .user(user)
                .email(email)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .isUsed(false)
                .build();
        passwordResetTokenRepository.save(resetToken);

        // Send email / log URL
        emailService.sendPasswordResetEmail(email, token);

        return ApiResponse.success(
                NEUTRAL_FORGOT_PASSWORD_MESSAGE,
                null
        );
    }

    @Override
    @Transactional
    public ResetPasswordResponse resetPassword(ResetPasswordRequest request) {
        // 1. Check password confirmation
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            Map<String, String> errors = new HashMap<>();
            errors.put("confirmPassword", "Mật khẩu xác nhận không khớp với mật khẩu mới");
            throw new ValidationFailedException("Mật khẩu xác nhận không khớp với mật khẩu mới", errors);
        }

        // 2. Validate password policy
        if (!PasswordValidator.isValid(request.getNewPassword())) {
            Map<String, String> errors = new HashMap<>();
            errors.put("newPassword", "Mật khẩu phải có tối thiểu 8 ký tự, bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt");
            throw new ValidationFailedException("Dữ liệu đầu vào không hợp lệ", errors);
        }

        // 3. Find token
        PasswordResetTokenJpaEntity resetToken = passwordResetTokenRepository
                .findByToken(hashToken(request.getToken()))
                .orElseThrow(InvalidResetTokenException::new);

        if (resetToken.isUsed() || resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidResetTokenException();
        }

        UserJpaEntity user = resetToken.getUser();

        // 4. Check new password != old password
        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new PasswordSameAsOldException();
        }

        // 5. Update user password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // 6. Mark token as used
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        // 7. Revoke all refresh tokens
        refreshTokenRepository.deleteByUserId(user.getId());

        return ResetPasswordResponse.builder()
                .userId(user.getId())
                .passwordResetAt(user.getUpdatedAt())
                .build();
    }

    // =========================================================================
    // Đăng xuất và vòng đời phiên đăng nhập (xoay vòng refresh token)
    // =========================================================================
    @Override
    @Transactional
    public ApiResponse<Void> logout(Long userId, String authorizationHeader, LogoutRequest request) {
        if (StringUtils.hasText(authorizationHeader) && authorizationHeader.startsWith("Bearer ")) {
            String jwt = authorizationHeader.substring(7);
            if (jwtTokenProvider.validateToken(jwt)) {
                String jti = jwtTokenProvider.getJtiFromToken(jwt);
                Date exp = jwtTokenProvider.getExpirationFromToken(jwt);
                LocalDateTime expiresAt = exp.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

                TokenBlacklistJpaEntity blacklist = TokenBlacklistJpaEntity.builder()
                        .jti(jti != null ? jti : jwt)
                        .userId(userId)
                        .expiresAt(expiresAt)
                        .build();
                tokenBlacklistRepository.save(blacklist);
            }
        }

        if (request != null && StringUtils.hasText(request.getRefreshToken())) {
            refreshTokenRepository.deleteByToken(request.getRefreshToken());
        }

        return ApiResponse.success("Đăng xuất thành công", null);
    }

    @Override
    @Transactional
    public AuthUseCase.AuthResult refreshToken(RefreshTokenRequest request) {
        RefreshTokenJpaEntity refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken()).orElse(null);
        UserJpaEntity user = null;
        if (refreshToken != null) {
            if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now()) || refreshToken.isRevoked()) {
                refreshTokenRepository.delete(refreshToken);
                throw new InvalidRefreshTokenException("Phiên đăng nhập đã hết hạn hoặc không hợp lệ. Vui lòng đăng nhập lại");
            }
            user = refreshToken.getUser();
            refreshTokenRepository.delete(refreshToken);
        } else if (jwtTokenProvider.validateToken(request.getRefreshToken())) {
            Long userId = jwtTokenProvider.getUserIdFromToken(request.getRefreshToken());
            user = userRepository.findById(userId).orElseThrow(InvalidRefreshTokenException::new);
        } else {
            throw new InvalidRefreshTokenException();
        }

        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new AccountBlockedException("Tài khoản của bạn đã bị khóa");
        }

        // Create new tokens
        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
        String newRefreshTokenString = UUID.randomUUID().toString();

        RefreshTokenJpaEntity newRefreshToken = RefreshTokenJpaEntity.builder()
                .token(newRefreshTokenString)
                .user(user)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        refreshTokenRepository.save(newRefreshToken);

        String fullName = user.getUsername();
        String avatarUrl = null;
        boolean isVerified = false;
        VerificationStatus verificationStatus = VerificationStatus.PENDING;

        if (user.getRole() == Role.CUSTOMER) {
            var custOpt = customerProfileRepository.findByUserId(user.getId());
            if (custOpt.isPresent()) {
                fullName = custOpt.get().getFullName();
                avatarUrl = custOpt.get().getAvatarUrl();
                isVerified = true;
                verificationStatus = VerificationStatus.APPROVED;
            }
        } else if (user.getRole() == Role.TECHNICIAN) {
            var techOpt = technicianProfileRepository.findByUserId(user.getId());
            if (techOpt.isPresent()) {
                fullName = techOpt.get().getFullName();
                avatarUrl = techOpt.get().getAvatarUrl();
                verificationStatus = techOpt.get().getVerificationStatus();
                isVerified = verificationStatus == VerificationStatus.APPROVED;
            }
        } else if (user.getRole() == Role.ADMIN) {
            isVerified = true;
            verificationStatus = VerificationStatus.APPROVED;
        }

        AuthUseCase.UserInfo userInfo = new AuthUseCase.UserInfo(
                "usr_" + user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getStatus(),
                fullName,
                avatarUrl,
                isVerified,
                verificationStatus
        );

        return new AuthUseCase.AuthResult(
                newAccessToken,
                newRefreshTokenString,
                "Bearer",
                jwtTokenProvider.getAccessTokenExpirationSeconds(),
                userInfo
        );
    }
}
