package com.fixlink.infrastructure.config;

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
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("FixLink API Documentation")
                        .version("1.0.0")
                        .description("Hệ thống API nền tảng kết nối khách hàng và kỹ thuật viên sửa chữa (FixLink). " +
                                "Áp dụng kiến trúc lục giác (Hexagonal Architecture) với cơ chế Đấu giá ngược và Ký quỹ Escrow.")
                        .contact(new Contact()
                                .name("Nhóm Đồ Án FixLink")
                                .email("support@fixlink.vn"))
                        .license(new License().name("Apache 2.0").url("https://springdoc.org")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Nhập Access Token được cấp sau khi đăng nhập: Bearer {token}")));
    }
}
