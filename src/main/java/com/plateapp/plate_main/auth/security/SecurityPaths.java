package com.plateapp.plate_main.auth.security;

public final class SecurityPaths {

    private SecurityPaths() {
    }

    public static final String[] PUBLIC_MATCHERS = {
            "/auth/**",
            "/email/**",
            "/api/auth/**",
            "/api/email/**",
            "/api/owner/business-verifications",
            "/api/owner/signup-applications",
            "/api/owner/signup-account-validations",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/swagger-ui/index.html"
    };
}
