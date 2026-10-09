package com.fixlink.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import jakarta.annotation.PostConstruct;
import java.net.URI;

@Configuration
@Profile("prod")
public class DatabaseUrlConfig {

    @PostConstruct
    public void parseDatabaseUrl() {
        String databaseUrl = System.getenv("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.isBlank()) {
            return;
        }

        URI uri = URI.create(databaseUrl.replace("postgres://", "postgresql://"));

        String jdbcUrl = "jdbc:postgresql://" + uri.getHost()
                + ":" + uri.getPort()
                + uri.getPath()
                + "?sslmode=require";

        String[] userInfo = uri.getUserInfo().split(":", 2);

        System.setProperty("JDBC_DATABASE_URL", jdbcUrl);
        System.setProperty("DATABASE_USERNAME", userInfo[0]);
        System.setProperty("DATABASE_PASSWORD", userInfo.length > 1 ? userInfo[1] : "");
    }
}
