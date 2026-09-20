package com.fixlink.application.port.in;

import com.fixlink.adapter.in.web.dto.request.ForgotPasswordRequest;
import com.fixlink.adapter.in.web.dto.request.ResetPasswordRequest;
import com.fixlink.adapter.in.web.dto.response.ApiResponse;
import com.fixlink.adapter.in.web.dto.response.ResetPasswordResponse;

public interface PasswordResetUseCase {
    ApiResponse<Void> forgotPassword(ForgotPasswordRequest request);
    ResetPasswordResponse resetPassword(ResetPasswordRequest request);
}
