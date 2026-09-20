package com.fixlink.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Trả 401 khi request chưa đăng nhập chạm vào tài nguyên cần xác thực.
 *
 * <p>Mặc định Spring Security trả 403 cho cả trường hợp thiếu token, khiến client
 * không phân biệt được "chưa đăng nhập" và "đăng nhập rồi nhưng không đủ quyền".
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                "{\"statusCode\":401,\"errorCode\":\"UNAUTHENTICATED\","
                        + "\"message\":\"Bạn cần đăng nhập để thực hiện thao tác này\"}"
        );
    }
}
