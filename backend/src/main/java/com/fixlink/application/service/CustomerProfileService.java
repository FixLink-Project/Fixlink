package com.fixlink.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixlink.adapter.out.persistence.entity.AuditLogJpaEntity;
import com.fixlink.adapter.out.persistence.entity.CustomerProfileJpaEntity;
import com.fixlink.adapter.out.persistence.mapper.UserMapper;
import com.fixlink.adapter.out.persistence.repository.SpringDataAuditLogRepository;
import com.fixlink.adapter.out.persistence.repository.SpringDataCustomerProfileRepository;
import com.fixlink.application.port.in.CustomerProfileUseCase;
import com.fixlink.application.port.in.CustomerProfileUseCase.Requester;
import com.fixlink.domain.exception.DomainException;
import com.fixlink.domain.exception.ResourceNotFoundException;
import com.fixlink.domain.model.CustomerProfile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class CustomerProfileService implements CustomerProfileUseCase {

    private final SpringDataCustomerProfileRepository customerProfileRepository;
    private final SpringDataAuditLogRepository auditLogRepository;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    public CustomerProfileService(SpringDataCustomerProfileRepository customerProfileRepository,
                                  SpringDataAuditLogRepository auditLogRepository,
                                  UserMapper userMapper,
                                  ObjectMapper objectMapper) {
        this.customerProfileRepository = customerProfileRepository;
        this.auditLogRepository = auditLogRepository;
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerProfile getProfile(Long targetUserId, Requester requester) {
        requireReadAccess(requester, targetUserId);

        CustomerProfileJpaEntity entity = customerProfileRepository.findByUserIdAndDeletedAtIsNull(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ khách hàng với ID: " + targetUserId));
        return userMapper.toDomain(entity);
    }

    /**
     * Chặn đọc hồ sơ của người khác (IDOR). Chỉ dựa vào token đã xác thực,
     * không dùng bất kỳ giá trị nào do client gửi lên.
     */
    private void requireReadAccess(Requester requester, Long targetUserId) {
        if (requester == null || !requester.canRead(targetUserId)) {
            throw new AccessDeniedException("Bạn không có quyền xem hồ sơ của người dùng khác");
        }
    }

    @Override
    public CustomerProfile updateProfile(Long targetUserId, Long currentUserId, UpdateProfileCommand command) {
        // 1. Kiểm tra IDOR - AC 2: User chỉ được cập nhật hồ sơ chính mình
        if (currentUserId == null || !currentUserId.equals(targetUserId)) {
            throw new AccessDeniedException("Bạn không có quyền chỉnh sửa hồ sơ của người dùng khác");
        }

        // 2. Tìm hồ sơ khách hàng hiện tại
        CustomerProfileJpaEntity entity = customerProfileRepository.findByUserIdAndDeletedAtIsNull(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ khách hàng với ID: " + targetUserId));

        // 3. Kiểm tra trùng lặp SĐT hoặc Email với người dùng khác
        if (!entity.getPhone().equals(command.phone()) &&
                customerProfileRepository.existsByPhoneAndUserIdNotAndDeletedAtIsNull(command.phone(), targetUserId)) {
            throw new DomainException("Số điện thoại " + command.phone() + " đã được sử dụng bởi người dùng khác");
        }

        if (!entity.getEmail().equalsIgnoreCase(command.email()) &&
                customerProfileRepository.existsByEmailAndUserIdNotAndDeletedAtIsNull(command.email(), targetUserId)) {
            throw new DomainException("Email " + command.email() + " đã được sử dụng bởi người dùng khác");
        }

        // 4. Lưu lại giá trị cũ để ghi Audit Trail
        Map<String, Object> oldValues = new LinkedHashMap<>();
        oldValues.put("fullName", entity.getFullName());
        oldValues.put("phone", entity.getPhone());
        oldValues.put("email", entity.getEmail());
        oldValues.put("avatarUrl", entity.getAvatarUrl());

        // 5. Cập nhật các trường thông tin hồ sơ
        entity.setFullName(command.fullName().trim());
        entity.setPhone(command.phone().trim());
        entity.setEmail(command.email().trim().toLowerCase());
        if (command.avatarUrl() != null && !command.avatarUrl().isBlank()) {
            entity.setAvatarUrl(command.avatarUrl().trim());
        }
        entity.setUpdatedBy(currentUserId != null ? currentUserId : targetUserId);

        // Flush ngay de @PreUpdate cap nhat updatedAt truoc khi map sang DTO tra ve.
        // Neu chi save(), UPDATE chi chay luc commit nen response mang timestamp cu.
        CustomerProfileJpaEntity savedEntity = customerProfileRepository.saveAndFlush(entity);

        // 6. Ghi nhận thay đổi vào Audit Trail
        Map<String, Object> newValues = new LinkedHashMap<>();
        newValues.put("fullName", savedEntity.getFullName());
        newValues.put("phone", savedEntity.getPhone());
        newValues.put("email", savedEntity.getEmail());
        newValues.put("avatarUrl", savedEntity.getAvatarUrl());

        try {
            String oldValuesJson = objectMapper.writeValueAsString(oldValues);
            String newValuesJson = objectMapper.writeValueAsString(newValues);

            AuditLogJpaEntity auditLog = new AuditLogJpaEntity(
                    currentUserId != null ? currentUserId : targetUserId,
                    "UPDATE_CUSTOMER_PROFILE",
                    "customer_profiles",
                    String.valueOf(targetUserId),
                    oldValuesJson,
                    newValuesJson
            );
            auditLogRepository.save(auditLog);
        } catch (JsonProcessingException e) {
            // Không làm gián đoạn transaction nếu parse json gặp sự cố
        }

        return userMapper.toDomain(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogEntry> getAuditTrail(Long targetUserId, Requester requester) {
        requireReadAccess(requester, targetUserId);

        return auditLogRepository.findByEntityNameAndEntityIdOrderByCreatedAtDesc("customer_profiles", String.valueOf(targetUserId))
                .stream()
                .map(entity -> new AuditLogEntry(
                        entity.getId(),
                        entity.getUserId(),
                        entity.getAction(),
                        entity.getEntityName(),
                        entity.getEntityId(),
                        entity.getOldValues(),
                        entity.getNewValues(),
                        entity.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }
}
