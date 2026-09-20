package com.fixlink.application.port.in;

import com.fixlink.adapter.in.web.dto.request.ChangePasswordRequest;
import com.fixlink.adapter.in.web.dto.response.ChangePasswordResponse;

public interface ChangePasswordUseCase {
    ChangePasswordResponse changePassword(Long userId, ChangePasswordRequest request);
}
