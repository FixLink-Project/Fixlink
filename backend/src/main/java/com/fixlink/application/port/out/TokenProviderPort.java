package com.fixlink.application.port.out;

import com.fixlink.domain.model.Role;

public interface TokenProviderPort {
    String generateAccessToken(Long userId, String username, Role role);
    String generateRefreshToken(Long userId, String username);
    long getAccessTokenExpirationSeconds();
}
