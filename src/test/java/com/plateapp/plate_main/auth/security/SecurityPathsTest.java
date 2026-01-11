package com.plateapp.plate_main.auth.security;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class SecurityPathsTest {

    @Test
    void publicMatchersContainExpectedRoutes() {
        List<String> matchers = Arrays.asList(SecurityPaths.PUBLIC_MATCHERS);

        assertTrue(matchers.contains("/auth/**"));
        assertTrue(matchers.contains("/email/**"));
        assertTrue(matchers.contains("/v3/api-docs/**"));
        assertTrue(matchers.contains("/swagger-ui/**"));
    }
}
