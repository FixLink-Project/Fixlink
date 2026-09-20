package com.fixlink.application.service;

import com.fixlink.application.port.in.TechnicianVerificationUseCase;
import com.fixlink.application.port.in.UserManagementUseCase;
import com.fixlink.application.port.out.TechnicianProfileRepositoryPort;
import com.fixlink.application.port.out.UserRepositoryPort;
import com.fixlink.domain.exception.ResourceNotFoundException;
import com.fixlink.domain.model.TechnicianProfile;
import com.fixlink.domain.model.User;
import com.fixlink.domain.model.UserStatus;
import com.fixlink.domain.model.VerificationStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserManagementService implements UserManagementUseCase, TechnicianVerificationUseCase {

    private final UserRepositoryPort userRepository;
    private final TechnicianProfileRepositoryPort technicianProfileRepository;

    public UserManagementService(UserRepositoryPort userRepository,
                                 TechnicianProfileRepositoryPort technicianProfileRepository) {
        this.userRepository = userRepository;
        this.technicianProfileRepository = technicianProfileRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserPageResult getUsers(UserQuery query) {
        return userRepository.findAll(query);
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + id));
    }

    @Override
    @Transactional
    public void updateUserStatus(Long userId, UserStatus status) {
        User user = getUserById(userId);
        user.setStatus(status);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public VerificationResult verifyTechnician(VerifyTechnicianCommand command) {
        User user = getUserById(command.technicianUserId());

        TechnicianProfile profile = technicianProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ kỹ thuật viên cho người dùng ID: " + command.technicianUserId()));

        if (command.status() == VerificationStatus.APPROVED) {
            profile.verify(command.adminUserId());
            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);
        } else if (command.status() == VerificationStatus.REJECTED) {
            profile.reject(command.adminUserId(), command.rejectionReason());
        }

        technicianProfileRepository.save(profile);

        return new VerificationResult(
                "usr_" + user.getId(),
                profile.getVerificationStatus(),
                profile.getVerifiedAt(),
                command.adminUserId() != null ? "admin_usr_" + command.adminUserId() : "admin"
        );
    }
}
