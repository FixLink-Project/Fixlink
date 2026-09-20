package com.fixlink.adapter.in.web.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogoutRequest {
    private String refreshToken;
    private String deviceToken;

    public LogoutRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
