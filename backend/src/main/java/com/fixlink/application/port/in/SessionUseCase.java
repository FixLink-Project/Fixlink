package com.fixlink.application.port.in;

import com.fixlink.adapter.in.web.dto.request.LogoutRequest;
import com.fixlink.adapter.in.web.dto.request.RefreshTokenRequest;
import com.fixlink.adapter.in.web.dto.response.ApiResponse;

public interface SessionUseCase {
    ApiResponse<Void> logout(Long userId, String authorizationHeader, LogoutRequest request);
    AuthUseCase.AuthResult refreshToken(RefreshTokenRequest request);
}
