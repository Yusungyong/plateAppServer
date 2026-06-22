package com.plateapp.plate_main.auth.security;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(
        List<String> allowedOrigins,
        boolean allowCredentials,
        Duration maxAge
) {
    public CorsProperties {
        allowedOrigins = allowedOrigins == null ? List.of() : allowedOrigins.stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
        maxAge = maxAge == null ? Duration.ofHours(1) : maxAge;
    }
}
