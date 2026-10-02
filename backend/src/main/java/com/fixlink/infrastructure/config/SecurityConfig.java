package com.fixlink.infrastructure.config;

import com.fixlink.infrastructure.security.JwtAuthenticationFilter;
import com.fixlink.infrastructure.security.RestAccessDeniedHandler;
import com.fixlink.infrastructure.security.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            RestAuthenticationEntryPoint restAuthenticationEntryPoint,
            RestAccessDeniedHandler restAccessDeniedHandler
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.restAuthenticationEntryPoint = restAuthenticationEntryPoint;
        this.restAccessDeniedHandler = restAccessDeniedHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Tài nguyên tĩnh và tài liệu API
                        .requestMatchers(
                                "/",
                                "/*.html",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/favicon.ico",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml",
                                "/h2-console/**"
                        ).permitAll()

                        // Healthcheck cho container và pipeline triển khai
                        .requestMatchers(HttpMethod.GET, "/api/v1/health").permitAll()

                        // VIEC 2 - Cách 1 (tuân thủ tuyệt đối yêu cầu giảng viên):
                        // TRỪ đăng nhập, mọi endpoint nghiệp vụ đều phải authenticated.
                        // Đây là endpoint công khai DUY NHẤT của luồng nghiệp vụ.
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()

                        // Tạo tài khoản khách/thợ giờ do ADMIN thực hiện (không self-register).
                        .requestMatchers("/api/v1/auth/register/**").hasRole("ADMIN")

                        // Dữ liệu master công khai: trang chủ (landing) cho khách chưa đăng nhập
                        // vẫn cần đọc danh mục ngành nghề / gói dịch vụ / khu vực để hiển thị.
                        // Chỉ mở các GET này; mọi thao tác ghi master-data vẫn qua /admin/**.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/categories/**",
                                "/api/v1/services/**",
                                "/api/v1/areas/**")
                        .permitAll()

                        // Quên/đặt lại mật khẩu: người dùng chưa đăng nhập được mới cần, nên mở
                        // công khai. (change-password vẫn yêu cầu đăng nhập; refresh-token vẫn
                        //  rơi xuống anyRequest().authenticated() bên dưới.)
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/auth/forgot-password",
                                "/api/v1/auth/reset-password")
                        .permitAll()

                        // Phân quyền theo vai trò
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/staff/**").hasAnyRole("STAFF", "ADMIN")
                        .requestMatchers("/api/v1/technicians/**").hasAnyRole("TECHNICIAN", "ADMIN")
                        // Quyền đọc hồ sơ của người khác còn được siết thêm một lớp nữa
                        // ở CustomerProfileService: khách chỉ xem được hồ sơ chính mình.
                        .requestMatchers("/api/v1/customers/**").hasAnyRole("CUSTOMER", "STAFF", "ADMIN")

                        // Mọi đường dẫn còn lại đều phải đăng nhập
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        // Thiếu token trả 401, sai vai trò trả 403 (mặc định Spring trả 403 cho cả hai)
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler)
                )
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
