package com.fixlink.service;

import com.fixlink.dto.response.*;
import com.fixlink.entity.CustomerProfile;
import com.fixlink.entity.TechnicianProfile;
import com.fixlink.entity.User;
import com.fixlink.enums.UserRole;
import com.fixlink.enums.UserStatus;
import com.fixlink.enums.VerificationStatus;
import com.fixlink.exception.InvalidOperationException;
import com.fixlink.exception.ResourceNotFoundException;
import com.fixlink.repository.TechnicianProfileRepository;
import com.fixlink.repository.UserRepository;
import com.fixlink.specification.UserSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final TechnicianProfileRepository technicianProfileRepository;

    public AdminUserService(UserRepository userRepository,
                            TechnicianProfileRepository technicianProfileRepository) {
        this.userRepository = userRepository;
        this.technicianProfileRepository = technicianProfileRepository;
    }

    // ===================================================================
    // RC-19: Admin View & Search User List
    // ===================================================================
    @Transactional(readOnly = true)
    public PaginatedResponse<UserDto> getUsers(int page, int limit,
                                                String search, String role, String status,
                                                String sortBy, String sortOrder) {
        // Validate & cap limit
        page = Math.max(1, page);
        limit = Math.min(Math.max(1, limit), 100);

        // Map sortBy from API field names to entity field names
        String entitySortField = mapSortField(sortBy);
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortOrder)
                ? Sort.Direction.ASC : Sort.Direction.DESC;

        // Spring Data pages are 0-based
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(direction, entitySortField));

        // Build dynamic specification
        Specification<User> spec = UserSpecification.buildSpec(search, role, status);

        Page<User> userPage = userRepository.findAll(spec, pageable);

        List<UserDto> userDtos = userPage.getContent().stream()
                .map(this::toUserDto)
                .collect(Collectors.toList());

        return PaginatedResponse.of(
                "Lấy danh sách người dùng thành công",
                userDtos,
                page,
                limit,
                userPage.getTotalElements(),
                userPage.getTotalPages()
        );
    }

    // ===================================================================
    // RC-20: Admin View User Detail
    // ===================================================================
    @Transactional(readOnly = true)
    public ApiResponse<UserDto> getUserById(Long id) {
        User user = userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy người dùng với ID: " + id));

        return ApiResponse.success("Thành công", toUserDto(user));
    }

    // ===================================================================
    // RC-21: Admin Block / Unblock User
    // ===================================================================
    @Transactional
    public ApiResponse<StatusUpdateResponse> updateUserStatus(Long id, String apiStatus,
                                                               String reason, Long adminId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy người dùng với ID: " + id));

        // Don't allow blocking other admins
        if (user.getRole() == UserRole.ADMIN) {
            throw new InvalidOperationException(
                    "Không thể thay đổi trạng thái tài khoản Admin");
        }

        // Map API status to DB status
        UserStatus newStatus;
        if ("BLOCKED".equalsIgnoreCase(apiStatus)) {
            if (reason == null || reason.trim().length() < 10) {
                throw new InvalidOperationException(
                        "Lý do khóa tài khoản là bắt buộc và phải có ít nhất 10 ký tự",
                        "VALIDATION_ERROR");
            }
            newStatus = UserStatus.BANNED;
        } else if ("ACTIVE".equalsIgnoreCase(apiStatus)) {
            newStatus = UserStatus.ACTIVE;
        } else {
            throw new InvalidOperationException("Trạng thái không hợp lệ: " + apiStatus);
        }

        // Update user status
        user.setStatus(newStatus);
        user.setStatusChangedBy(adminId);
        user.setStatusChangedAt(LocalDateTime.now());
        user.setStatusReason(reason);

        // RC-21 acceptance: If blocking technician, their verification is no longer effective
        if (newStatus == UserStatus.BANNED && user.getRole() == UserRole.TECHNICIAN) {
            TechnicianProfile techProfile = technicianProfileRepository
                    .findByUserId(user.getId()).orElse(null);
            if (techProfile != null) {
                techProfile.setIsVerified(false);
                techProfile.setVerificationStatus(VerificationStatus.PENDING);
                technicianProfileRepository.save(techProfile);
            }
        }

        userRepository.save(user);

        StatusUpdateResponse responseData = StatusUpdateResponse.builder()
                .userId("usr_" + user.getId())
                .status(mapDbStatusToApi(newStatus))
                .build();

        return ApiResponse.success("Cập nhật trạng thái tài khoản thành công", responseData);
    }

    // ===================================================================
    // RC-22: Admin Verify Technician Profile (KYC)
    // ===================================================================
    @Transactional
    public ApiResponse<StatusUpdateResponse> verifyTechnician(Long userId,
                                                               String verificationStatusStr,
                                                               String note,
                                                               String rejectionReason,
                                                               Long adminId) {
        // Find user and validate role
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy người dùng với ID: " + userId));

        if (user.getRole() != UserRole.TECHNICIAN) {
            throw new InvalidOperationException(
                    "Người dùng này không phải là Kỹ thuật viên");
        }

        TechnicianProfile techProfile = technicianProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy hồ sơ kỹ thuật viên cho user ID: " + userId));

        VerificationStatus newVerificationStatus = VerificationStatus.valueOf(
                verificationStatusStr.toUpperCase());

        // Validate: only PENDING profiles can be verified
        if (techProfile.getVerificationStatus() != null
                && techProfile.getVerificationStatus() != VerificationStatus.PENDING) {
            throw new InvalidOperationException(
                    "Hồ sơ kỹ thuật viên đã được xử lý trước đó (trạng thái: "
                    + techProfile.getVerificationStatus() + ")");
        }

        // RC-22: REJECTED requires rejection reason
        if (newVerificationStatus == VerificationStatus.REJECTED
                && (rejectionReason == null || rejectionReason.isBlank())) {
            throw new InvalidOperationException(
                    "Phải cung cấp lý do từ chối khi reject hồ sơ KYC",
                    "INVALID_VERIFICATION_REQUEST");
        }

        // Update verification status
        techProfile.setVerificationStatus(newVerificationStatus);
        techProfile.setVerifiedBy(adminId);
        techProfile.setVerifiedAt(LocalDateTime.now());
        techProfile.setVerificationNote(note);

        if (newVerificationStatus == VerificationStatus.APPROVED) {
            techProfile.setIsVerified(true);
            // Activate user account when approved
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);
        } else if (newVerificationStatus == VerificationStatus.REJECTED) {
            techProfile.setIsVerified(false);
            techProfile.setRejectionReason(rejectionReason);
        }

        technicianProfileRepository.save(techProfile);

        StatusUpdateResponse responseData = StatusUpdateResponse.builder()
                .userId("usr_" + userId)
                .verificationStatus(newVerificationStatus.name())
                .verifiedAt(techProfile.getVerifiedAt())
                .verifiedBy("admin_usr_" + adminId)
                .build();

        return ApiResponse.success(
                "Cập nhật trạng thái xác minh thợ thành công", responseData);
    }

    // ===================================================================
    // Helper methods: Entity → DTO mapping
    // ===================================================================

    private UserDto toUserDto(User user) {
        UserDto.UserDtoBuilder builder = UserDto.builder()
                .id("usr_" + user.getId())
                .username(user.getUsername())
                .role(user.getRole().name())
                .status(mapDbStatusToApi(user.getStatus()))
                .createdAt(user.getCreatedAt());

        // Attach profile based on role
        if (user.getRole() == UserRole.CUSTOMER && user.getCustomerProfile() != null) {
            CustomerProfile cp = user.getCustomerProfile();
            builder.customerProfile(UserDto.CustomerProfileDto.builder()
                    .fullName(cp.getName())
                    .phone(cp.getPhone())
                    .email(cp.getEmail())
                    .avatarUrl(cp.getAvatarUrl())
                    .membershipTier(deriveMembershipTier(cp.getLoyaltyPoints()))
                    .build());
        } else if (user.getRole() == UserRole.TECHNICIAN && user.getTechnicianProfile() != null) {
            TechnicianProfile tp = user.getTechnicianProfile();
            builder.technicianProfile(UserDto.TechnicianProfileDto.builder()
                    .fullName(tp.getName())
                    .phone(tp.getPhone())
                    .email(tp.getEmail())
                    .citizenId(tp.getIdCardNumber())
                    .avatarUrl(tp.getAvatarUrl())
                    .verificationStatus(tp.getVerificationStatus() != null
                            ? tp.getVerificationStatus().name() : "PENDING")
                    .yearsExperience(tp.getYearsExperience() != null
                            ? tp.getYearsExperience().intValue() : 0)
                    .avgRating(tp.getAvgRating())
                    .completedJobs(tp.getCompletedJobs())
                    .walletBalance(0L) // Wallet balance computed from transactions
                    .isOnline(false)   // Online status from real-time service
                    .build());
        }

        return builder.build();
    }

    /**
     * Map DB status (BANNED) → API status (BLOCKED).
     */
    private String mapDbStatusToApi(UserStatus dbStatus) {
        return switch (dbStatus) {
            case BANNED -> "BLOCKED";
            case INACTIVE -> "PENDING";
            case ACTIVE -> "ACTIVE";
        };
    }

    /**
     * Derive membership tier from loyalty points.
     */
    private String deriveMembershipTier(Integer loyaltyPoints) {
        if (loyaltyPoints == null) return "STANDARD";
        if (loyaltyPoints >= 100) return "VIP";
        if (loyaltyPoints >= 50) return "GOLD";
        return "STANDARD";
    }

    /**
     * Map sort field name from API to entity property.
     */
    private String mapSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) return "createdAt";
        return switch (sortBy.toLowerCase()) {
            case "createdat", "created_at" -> "createdAt";
            case "username" -> "username";
            case "role" -> "role";
            case "status" -> "status";
            default -> "createdAt";
        };
    }
}
