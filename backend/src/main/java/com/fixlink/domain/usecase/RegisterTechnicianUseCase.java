package com.fixlink.domain.usecase;

import com.fixlink.application.port.in.RegisterTechnicianUseCase.RegisterTechnicianCommand;
import com.fixlink.application.port.in.RegisterTechnicianUseCase.TechnicianRegisterResult;

/**
 * Use case trong domain layer cho nghiệp vụ Thợ đăng ký tài khoản công khai.
 */
public interface RegisterTechnicianUseCase {
    TechnicianRegisterResult registerTechnician(RegisterTechnicianCommand command);
}
