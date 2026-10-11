package com.fixlink.infrastructure.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class DatabaseUrlConfig implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String databaseUrl = environment.getProperty("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.isBlank()) {
            return;
        }

        String normalized = databaseUrl;
        if (normalized.startsWith("postgres://")) {
            normalized = "postgresql://" + normalized.substring("postgres://".length());
        }

        URI uri = URI.create(normalized);

        int port = uri.getPort() > 0 ? uri.getPort() : 5432;

        String query = uri.getQuery();
        String params = (query != null && !query.isBlank()) ? "?" + query : "?sslmode=require";

        String jdbcUrl = "jdbc:postgresql://" + uri.getHost()
                + ":" + port
                + uri.getPath()
                + params;

        String[] userInfo = uri.getUserInfo().split(":", 2);

        Map<String, Object> props = new HashMap<>();
        props.put("spring.datasource.url", jdbcUrl);
        props.put("spring.datasource.username", userInfo[0]);
        props.put("spring.datasource.password", userInfo.length > 1 ? userInfo[1] : "");

        environment.getPropertySources()
                .addFirst(new MapPropertySource("databaseUrlConfig", props));
    }
}
