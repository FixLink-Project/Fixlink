package com.fixlink.application.service;

import com.fixlink.application.port.in.AuthUseCase;
import com.fixlink.application.port.out.CustomerProfileRepositoryPort;
import com.fixlink.application.port.out.PasswordEncoderPort;
import com.fixlink.application.port.out.TechnicianProfileRepositoryPort;
import com.fixlink.application.port.out.TokenProviderPort;
import com.fixlink.application.port.out.UserRepositoryPort;
import com.fixlink.domain.exception.AccountBlockedException;
import com.fixlink.domain.exception.AccountInactiveException;
import com.fixlink.domain.exception.AccountTemporarilyLockedException;
import com.fixlink.domain.exception.InvalidCredentialsException;
import com.fixlink.domain.exception.UserAlreadyExistsException;
import com.fixlink.domain.model.CustomerProfile;
import com.fixlink.domain.model.Role;
import com.fixlink.domain.model.TechnicianProfile;
import com.fixlink.domain.model.User;
import com.fixlink.domain.model.UserStatus;
import com.fixlink.domain.model.VerificationStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService implements AuthUseCase {

    private final UserRepositoryPort userRepository;
    private final CustomerProfileRepositoryPort customerProfileRepository;
    private final TechnicianProfileRepositoryPort technicianProfileRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final TokenProviderPort tokenProvider;
    private final LoginAttemptService loginAttemptService;

    public AuthService(UserRepositoryPort userRepository,
                       CustomerProfileRepositoryPort customerProfileRepository,
                       TechnicianProfileRepositoryPort technicianProfileRepository,
                       PasswordEncoderPort passwordEncoder,
                       TokenProviderPort tokenProvider,
                       LoginAttemptService loginAttemptService) {
        this.userRepository = userRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.technicianProfileRepository = technicianProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResult login(LoginCommand command) {
        String username = command.username() != null ? command.username().trim() : "";

        // Kiểm tra khóa tạm thời do brute-force nhập sai nhiều lần
        if (loginAttemptService.isBlocked(username)) {
            long remainingSeconds = loginAttemptService.getRemainingLockSeconds(username);
            long minutes = Math.max(1, (remainingSeconds + 59) / 60);
            throw new AccountTemporarilyLockedException(
                    "Bạn đã nhập sai mật khẩu quá 5 lần liên tiếp. Tài khoản tạm thời bị khóa trong " + minutes + " phút để bảo vệ an toàn.",
                    remainingSeconds
            );
        }

        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            int fails = loginAttemptService.recordFailure(username);
            if (fails >= LoginAttemptService.MAX_FAILED_ATTEMPTS) {
                throw new AccountTemporarilyLockedException(
                        "Bạn đã nhập sai mật khẩu quá 5 lần liên tiếp. Tài khoản tạm thời bị khóa trong 15 phút để bảo vệ an toàn."
                );
            }
            // Generic error, không tiết lộ tài khoản có tồn tại hay không
            throw new InvalidCredentialsException("Tên đăng nhập hoặc mật khẩu không chính xác");
        }

        // Kiểm tra tài khoản bị khóa hoặc ngừng hoạt động
        if (user.isBlocked()) {
            throw new AccountBlockedException("Tài khoản của bạn đã bị khóa do vi phạm chính sách");
        }
        if (user.isInactive()) {
            throw new AccountInactiveException("Tài khoản của bạn đang ở trạng thái ngừng hoạt động (Inactive). Vui lòng liên hệ quản trị viên.");
        }

        if (!passwordEncoder.matches(command.password(), user.getPasswordHash())) {
            int fails = loginAttemptService.recordFailure(username);
            if (fails >= LoginAttemptService.MAX_FAILED_ATTEMPTS) {
                throw new AccountTemporarilyLockedException(
                        "Bạn đã nhập sai mật khẩu quá 5 lần liên tiếp. Tài khoản tạm thời bị khóa trong 15 phút để bảo vệ an toàn."
                );
            }
            int remainingAttempts = LoginAttemptService.MAX_FAILED_ATTEMPTS - fails;
            // Generic error kèm số lần thử còn lại
            throw new InvalidCredentialsException(
                    "Tên đăng nhập hoặc mật khẩu không chính xác. Bạn còn " + remainingAttempts + " lần thử trước khi bị tạm khóa."
            );
        }

        // Đăng nhập thành công -> xóa bộ đếm thất bại
        loginAttemptService.recordSuccess(username);

        String fullName = user.getUsername();
        String avatarUrl = null;
        boolean isVerified = false;
        VerificationStatus verificationStatus = VerificationStatus.PENDING;

        if (user.getRole() == Role.CUSTOMER) {
            CustomerProfile profile = customerProfileRepository.findByUserId(user.getId()).orElse(null);
            if (profile != null) {
                fullName = profile.getFullName();
                avatarUrl = profile.getAvatarUrl();
                isVerified = true;
                verificationStatus = VerificationStatus.APPROVED;
            }
        } else if (user.getRole() == Role.TECHNICIAN) {
            TechnicianProfile profile = technicianProfileRepository.findByUserId(user.getId()).orElse(null);
            if (profile != null) {
                fullName = profile.getFullName();
                avatarUrl = profile.getAvatarUrl();
                verificationStatus = profile.getVerificationStatus() != null ? profile.getVerificationStatus() : VerificationStatus.PENDING;
                isVerified = profile.getVerificationStatus() == VerificationStatus.APPROVED;
            }
        } else {
            // ADMIN / STAFF
            isVerified = true;
            verificationStatus = VerificationStatus.APPROVED;
        }

        String accessToken = tokenProvider.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
        String refreshToken = tokenProvider.generateRefreshToken(user.getId(), user.getUsername());
        long expiresIn = tokenProvider.getAccessTokenExpirationSeconds();

        UserInfo userInfo = new UserInfo(
                "usr_" + user.getId(),
                user.getUsername(),
                user.getRole(),
                user.getStatus(),
                fullName,
                avatarUrl,
                isVerified,
                verificationStatus
        );

        return new AuthResult(accessToken, refreshToken, "Bearer", expiresIn, userInfo);
    }

    @Override
    @Transactional
    public CustomerRegisterResult registerCustomer(RegisterCustomerCommand command) {
        if (userRepository.existsByUsername(command.username())) {
            throw new UserAlreadyExistsException("Tên đăng nhập đã tồn tại trong hệ thống");
        }

        if (customerProfileRepository.existsByPhoneOrEmail(command.phone(), command.email())) {
            throw new UserAlreadyExistsException("Số điện thoại hoặc Email đã được sử dụng");
        }

        LocalDateTime now = LocalDateTime.now();
        User user = new User(
                null,
                command.username(),
                passwordEncoder.encode(command.password()),
                Role.CUSTOMER,
                UserStatus.ACTIVE,
                now,
                now
        );
        User savedUser = userRepository.save(user);

        CustomerProfile profile = new CustomerProfile(
                savedUser.getId(),
                command.fullName(),
                command.phone(),
                command.email(),
                null,
                "STANDARD",
                now,
                now
        );
        customerProfileRepository.save(profile);

        return new CustomerRegisterResult(
                "usr_" + savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getRole(),
                command.fullName(),
                command.phone(),
                command.email(),
                savedUser.getCreatedAt()
        );
    }

    @Override
    @Transactional
    public TechnicianRegisterResult registerTechnician(RegisterTechnicianCommand command) {
        if (userRepository.existsByUsername(command.username())) {
            throw new UserAlreadyExistsException("Tên đăng nhập đã tồn tại trong hệ thống");
        }

        if (technicianProfileRepository.existsByPhoneOrEmailOrCitizenId(command.phone(), command.email(), command.citizenId())) {
            throw new UserAlreadyExistsException("Số điện thoại, Email hoặc CCCD đã được sử dụng");
        }

        LocalDateTime now = LocalDateTime.now();
        User user = new User(
                null,
                command.username(),
                passwordEncoder.encode(command.password()),
                Role.TECHNICIAN,
                UserStatus.PENDING, // Chờ duyệt KYC
                now,
                now
        );
        User savedUser = userRepository.save(user);

        TechnicianProfile profile = new TechnicianProfile();
        profile.setUserId(savedUser.getId());
        profile.setFullName(command.fullName());
        profile.setPhone(command.phone());
        profile.setEmail(command.email());
        profile.setCitizenId(command.citizenId());
        profile.setIdCardFrontUrl(command.idCardFrontUrl());
        profile.setIdCardBackUrl(command.idCardBackUrl());
        profile.setBio(command.bio());
        profile.setYearsExperience(command.yearsExperience() != null ? command.yearsExperience() : 0);
        profile.setVerificationStatus(VerificationStatus.PENDING);
        profile.setCreatedAt(now);
        profile.setUpdatedAt(now);

        technicianProfileRepository.save(profile);

        return new TechnicianRegisterResult(
                "usr_" + savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getRole(),
                profile.getVerificationStatus(),
                savedUser.getCreatedAt()
        );
    }
}
