package com.fixlink.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI fixLinkOpenAPI() {
        final String securitySchemeName = "BearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("FixLink Platform REST API")
                        .description("Hệ thống API nền tảng kết nối khách hàng và kỹ thuật viên sửa chữa FixLink. " +
                                "Hỗ trợ xác thực JWT Bearer Token, quản lý danh mục, dịch vụ, tài khoản và đấu giá.")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("FixLink Engineering Team")
                                .email("dev@fixlink.vn")
                                .url("https://fixlink.vn"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://springdoc.org")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Nhập token JWT hợp lệ (không cần gõ tiền tố 'Bearer ')")));
    }
}
