package com.plateapp.plate_main.auth.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import io.jsonwebtoken.JwtException;

class JwtProviderTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    @Test
    void accessTokenContainsAccessType() {
        JwtProvider provider = new JwtProvider(SECRET);
        ReflectionTestUtils.setField(provider, "accessExpire", 1000L);
        ReflectionTestUtils.setField(provider, "refreshExpire", 2000L);

        String token = provider.createAccessToken("tester@example.com");

        assertEquals("tester@example.com", provider.getUsernameFromAccessToken(token));
        assertThrows(JwtException.class, () -> provider.getUsernameFromRefreshToken(token));
    }

    @Test
    void refreshTokenContainsRefreshType() {
        JwtProvider provider = new JwtProvider(SECRET);
        ReflectionTestUtils.setField(provider, "accessExpire", 1000L);
        ReflectionTestUtils.setField(provider, "refreshExpire", 2000L);

        String token = provider.createRefreshToken("tester@example.com");

        assertEquals("tester@example.com", provider.getUsernameFromRefreshToken(token));
        assertThrows(JwtException.class, () -> provider.getUsernameFromAccessToken(token));
    }

    @Test
    void validateReturnsFalseForMalformedToken() {
        JwtProvider provider = new JwtProvider(SECRET);
        ReflectionTestUtils.setField(provider, "accessExpire", 1000L);
        ReflectionTestUtils.setField(provider, "refreshExpire", 2000L);

        assertFalse(provider.validate("not-a-jwt"));
    }
}
