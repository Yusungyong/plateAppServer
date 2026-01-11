// src/main/java/com/plateapp/plate_main/auth/security/SecurityPaths.java
package com.plateapp.plate_main.auth.security;

public final class SecurityPaths {

    private SecurityPaths() {
    }

    public static final String[] PUBLIC_MATCHERS = {
            // 공개 API
            "/auth/**",
            "/email/**",

            // Swagger/OpenAPI
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/swagger-ui/index.html"
            // 필요시
            // "/webjars/**"
    };
}
