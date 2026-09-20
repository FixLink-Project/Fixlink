package com.fixlink.application.port.in;

import com.fixlink.domain.model.CustomerProfile;
import com.fixlink.domain.model.Role;

import java.time.LocalDateTime;
import java.util.List;

public interface CustomerProfileUseCase {

    CustomerProfile getProfile(Long targetUserId, Requester requester);

    CustomerProfile updateProfile(Long targetUserId, Long currentUserId, UpdateProfileCommand command);

    List<AuditLogEntry> getAuditTrail(Long targetUserId, Requester requester);

    /**
     * Người đang gọi API, dựng từ token đã xác thực.
     *
     * <p>Không bao giờ dựng từ dữ liệu client gửi lên (path, query hay header).
     */
    record Requester(Long userId, Role role) {

        /** Quản trị viên và nhân viên xem được hồ sơ bất kỳ; người khác chỉ xem của chính mình. */
        public boolean canRead(Long targetUserId) {
            if (role == Role.ADMIN || role == Role.STAFF) {
                return true;
            }
            return userId != null && userId.equals(targetUserId);
        }
    }

    record UpdateProfileCommand(
            String fullName,
            String phone,
            String email,
            String avatarUrl
    ) {}

    record AuditLogEntry(
            Long id,
            Long userId,
            String action,
            String entityName,
            String entityId,
            String oldValues,
            String newValues,
            LocalDateTime createdAt
    ) {}
}
