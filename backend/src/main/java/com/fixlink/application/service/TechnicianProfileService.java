package com.fixlink.application.service;

import com.fixlink.adapter.in.web.dto.request.UpdateTechnicianProfileRequest;
import com.fixlink.adapter.in.web.dto.response.AreaSimpleResponse;
import com.fixlink.adapter.in.web.dto.response.CategorySimpleResponse;
import com.fixlink.adapter.in.web.dto.response.TechnicianProfileResponse;
import com.fixlink.adapter.out.persistence.entity.ServiceAreaJpaEntity;
import com.fixlink.adapter.out.persistence.entity.ServiceCategoryJpaEntity;
import com.fixlink.adapter.out.persistence.entity.TechnicianProfileJpaEntity;
import com.fixlink.adapter.out.persistence.repository.SpringDataCustomerProfileRepository;
import com.fixlink.adapter.out.persistence.repository.SpringDataServiceAreaRepository;
import com.fixlink.adapter.out.persistence.repository.SpringDataServiceCategoryRepository;
import com.fixlink.adapter.out.persistence.repository.SpringDataTechnicianProfileRepository;
import com.fixlink.application.port.in.TechnicianProfileUseCase;
import com.fixlink.domain.exception.ResourceNotFoundException;
import com.fixlink.domain.exception.ValidationFailedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TechnicianProfileService implements TechnicianProfileUseCase {

    private final SpringDataTechnicianProfileRepository technicianProfileRepository;
    private final SpringDataCustomerProfileRepository customerProfileRepository;
    private final SpringDataServiceCategoryRepository serviceCategoryRepository;
    private final SpringDataServiceAreaRepository serviceAreaRepository;

    @Override
    @Transactional(readOnly = true)
    public TechnicianProfileResponse getMyProfile(Long userId) {
        TechnicianProfileJpaEntity profile = technicianProfileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ kỹ thuật viên"));
        return mapToResponse(profile, loadCategories(profile.getCategoryIds()), loadAreas(profile.getAreaIds()));
    }

    /** Đọc chi tiết nhóm việc từ danh sách id đã lưu ở bảng nối. */
    private List<CategorySimpleResponse> loadCategories(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return ids.stream()
                .map(serviceCategoryRepository::findByIdAndDeletedAtIsNull)
                .flatMap(Optional::stream)
                .map(c -> new CategorySimpleResponse(c.getId(), c.getName(), c.getIconUrl()))
                .toList();
    }

    /** Đọc chi tiết địa bàn từ danh sách id đã lưu ở bảng nối. */
    private List<AreaSimpleResponse> loadAreas(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return ids.stream()
                .map(serviceAreaRepository::findByIdAndIsActiveTrue)
                .flatMap(Optional::stream)
                .map(a -> new AreaSimpleResponse(a.getId(), a.getCode(), a.getName(), a.getCity()))
                .toList();
    }

    @Override
    @Transactional
    public TechnicianProfileResponse updateMyProfile(Long userId, UpdateTechnicianProfileRequest request) {
        TechnicianProfileJpaEntity profile = technicianProfileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ kỹ thuật viên"));

        Map<String, String> errors = new HashMap<>();

        // Validate duplicate phone
        if (technicianProfileRepository.existsByPhoneAndUserIdNot(request.getPhone(), userId) ||
                customerProfileRepository.existsByPhone(request.getPhone())) {
            errors.put("phone", "Số điện thoại đã được sử dụng bởi người dùng khác");
        }

        // Validate duplicate email
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            if (technicianProfileRepository.existsByEmailAndUserIdNot(request.getEmail(), userId) ||
                    customerProfileRepository.existsByEmail(request.getEmail())) {
                errors.put("email", "Email đã được sử dụng bởi người dùng khác");
            }
        }

        // Validate yearsExperience
        if (request.getYearsExperience() != null && (request.getYearsExperience() < 0 || request.getYearsExperience() > 50)) {
            errors.put("yearsExperience", "Số năm kinh nghiệm phải từ 0 đến 50");
        }

        // Validate categoryIds
        List<CategorySimpleResponse> categories = new ArrayList<>();
        if (request.getCategoryIds() != null) {
            for (Long catId : request.getCategoryIds()) {
                Optional<ServiceCategoryJpaEntity> catOpt = serviceCategoryRepository.findByIdAndDeletedAtIsNull(catId);
                if (catOpt.isEmpty() || !Boolean.TRUE.equals(catOpt.get().getIsActive())) {
                    errors.put("categoryIds", "Danh mục dịch vụ với ID = " + catId + " không tồn tại hoặc đã bị vô hiệu hóa");
                    break;
                }
                categories.add(new CategorySimpleResponse(catOpt.get().getId(), catOpt.get().getName(), catOpt.get().getIconUrl()));
            }
        }

        // Validate areaIds
        List<AreaSimpleResponse> areas = new ArrayList<>();
        if (request.getAreaIds() != null) {
            for (Long areaId : request.getAreaIds()) {
                Optional<ServiceAreaJpaEntity> areaOpt = serviceAreaRepository.findByIdAndIsActiveTrue(areaId);
                if (areaOpt.isEmpty()) {
                    errors.put("areaIds", "Khu vực hoạt động với ID = " + areaId + " không tồn tại hoặc đã bị vô hiệu hóa");
                    break;
                }
                areas.add(new AreaSimpleResponse(areaOpt.get().getId(), areaOpt.get().getCode(), areaOpt.get().getName(), areaOpt.get().getCity()));
            }
        }

        if (!errors.isEmpty()) {
            throw new ValidationFailedException("Dữ liệu đầu vào không hợp lệ", errors);
        }

        // Update mutable fields (preserving protected fields: verificationStatus, avgRating, completedJobs, walletBalance, citizenId)
        profile.setFullName(request.getFullName().trim());
        profile.setPhone(request.getPhone().trim());
        if (request.getEmail() != null) {
            profile.setEmail(request.getEmail().trim());
        }
        if (request.getBio() != null) {
            profile.setBio(request.getBio());
        }
        if (request.getYearsExperience() != null) {
            profile.setYearsExperience(request.getYearsExperience());
        }
        if (request.getAvatarUrl() != null) {
            profile.setAvatarUrl(request.getAvatarUrl());
        }

        // Chỉ ghi đè khi client gửi lên, để request không kèm trường này không xoá mất lựa chọn cũ.
        if (request.getCategoryIds() != null) {
            profile.setCategoryIds(new LinkedHashSet<>(request.getCategoryIds()));
        }
        if (request.getAreaIds() != null) {
            profile.setAreaIds(new LinkedHashSet<>(request.getAreaIds()));
        }

        profile.setUpdatedBy(userId);
        profile.setUpdatedAt(LocalDateTime.now());
        technicianProfileRepository.save(profile);

        return mapToResponse(profile, categories, areas);
    }

    private TechnicianProfileResponse mapToResponse(
            TechnicianProfileJpaEntity profile,
            List<CategorySimpleResponse> categories,
            List<AreaSimpleResponse> areas
    ) {
        return TechnicianProfileResponse.builder()
                .userId(profile.getUserId())
                .fullName(profile.getFullName())
                .phone(profile.getPhone())
                .email(profile.getEmail())
                .avatarUrl(profile.getAvatarUrl())
                .citizenId(profile.getCitizenId())
                .idCardFrontUrl(profile.getIdCardFrontUrl())
                .idCardBackUrl(profile.getIdCardBackUrl())
                .bio(profile.getBio())
                .yearsExperience(profile.getYearsExperience())
                .verificationStatus(profile.getVerificationStatus())
                .isOnline(profile.getIsOnline())
                .avgRating(profile.getAvgRating())
                .completedJobs(profile.getCompletedJobs())
                .walletBalance(profile.getWalletBalance())
                .categories(categories)
                .areas(areas)
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}
