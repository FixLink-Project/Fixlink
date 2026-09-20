package com.fixlink.application.service;

import com.fixlink.application.port.in.TechnicianUseCase;
import com.fixlink.application.port.out.TechnicianProfileRepositoryPort;
import com.fixlink.application.port.out.UserRepositoryPort;
import com.fixlink.domain.exception.ResourceNotFoundException;
import com.fixlink.domain.exception.UnverifiedTechnicianException;
import com.fixlink.domain.model.TechnicianProfile;
import com.fixlink.domain.model.VerificationStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TechnicianService implements TechnicianUseCase {

    private final TechnicianProfileRepositoryPort technicianProfileRepository;
    private final UserRepositoryPort userRepository;

    public TechnicianService(TechnicianProfileRepositoryPort technicianProfileRepository,
                             UserRepositoryPort userRepository) {
        this.technicianProfileRepository = technicianProfileRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public TechnicianProfile getMyProfile(Long userId) {
        return technicianProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ kỹ thuật viên cho người dùng ID: " + userId));
    }

    @Override
    @Transactional
    public TechnicianProfile updateProfile(Long userId, UpdateTechnicianProfileCommand command) {
        TechnicianProfile profile = getMyProfile(userId);

        profile.setFullName(command.fullName().trim());
        profile.setPhone(command.phone().trim());
        profile.setEmail(command.email().trim().toLowerCase());
        if (command.bio() != null) {
            profile.setBio(command.bio().trim());
        }
        if (command.yearsExperience() != null) {
            profile.setYearsExperience(command.yearsExperience());
        }
        if (command.avatarUrl() != null && !command.avatarUrl().isBlank()) {
            profile.setAvatarUrl(command.avatarUrl().trim());
        }

        return technicianProfileRepository.save(profile);
    }

    @Override
    @Transactional
    public TechnicianProfile updateOnlineStatus(Long userId, boolean isOnline) {
        TechnicianProfile profile = getMyProfile(userId);

        // AC 5: Chặn thợ chưa được Admin phê duyệt KYC nhận việc
        if (profile.getVerificationStatus() != VerificationStatus.APPROVED) {
            throw new UnverifiedTechnicianException(
                    "Tài khoản kỹ thuật viên đang ở trạng thái CHỜ XÁC MINH (" + profile.getVerificationStatus()
                    + "). Bạn không thể nhận yêu cầu sửa chữa cho đến khi Admin phê duyệt hồ sơ."
            );
        }

        profile.setIsOnline(isOnline);
        return technicianProfileRepository.save(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RepairRequestDto> getMyRepairRequests(Long userId) {
        TechnicianProfile profile = getMyProfile(userId);

        // AC 5: Chặn thợ chưa được Admin phê duyệt KYC xem / nhận yêu cầu sửa chữa
        if (profile.getVerificationStatus() != VerificationStatus.APPROVED) {
            throw new UnverifiedTechnicianException(
                    "Tài khoản chưa được xác minh. Bạn không thể nhận hoặc xem các yêu cầu sửa chữa cho đến khi Admin phê duyệt hồ sơ."
            );
        }

        return List.of(
                new RepairRequestDto(
                        "REQ-1001",
                        "Nguyễn Hoàng Nam",
                        "0912345678",
                        "Sửa máy lạnh Daikin Inverter 1.5HP rò rỉ nước",
                        "123 Cầu Giấy, P. Dịch Vọng, Cầu Giấy, Hà Nội",
                        "Dàn lạnh bị chảy nước xuống sàn gỗ, cần thợ kiểm tra gấp trong chiều nay",
                        "NEW",
                        "Hôm nay, 15:30"
                ),
                new RepairRequestDto(
                        "REQ-1002",
                        "Trần Thu Hương",
                        "0987654321",
                        "Bảo dưỡng máy giặt lồng ngang Electrolux",
                        "45 Nguyễn Trãi, Thanh Xuân, Hà Nội",
                        "Máy giặt rung lắc mạnh và kêu to khi vào chu trình vắt",
                        "NEW",
                        "Ngày mai, 09:00"
                )
        );
    }
}
