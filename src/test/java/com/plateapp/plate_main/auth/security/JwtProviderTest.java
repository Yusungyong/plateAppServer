package com.plateapp.plate_main.auth.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.plateapp.plate_main.auth.domain.User;
import io.jsonwebtoken.JwtException;
import java.util.List;

class JwtProviderTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    @Test
    void accessTokenContainsAccessType() {
        JwtProvider provider = new JwtProvider(SECRET);
        ReflectionTestUtils.setField(provider, "accessExpire", 60000L);
        ReflectionTestUtils.setField(provider, "refreshExpire", 120000L);

        String token = provider.createAccessToken("tester@example.com", "993");

        assertEquals("tester@example.com", provider.getUsernameFromAccessToken(token));
        assertEquals("ADMIN", provider.getRoleFromAccessToken(token));
        assertEquals("ADMIN", provider.getRolesFromAccessToken(token).get(0));
        assertTrue(provider.getPermissionsFromAccessToken(token).contains("ADMIN_ACCESS"));
        assertTrue(provider.getPermissionsFromAccessToken(token).contains("STORE_APPROVE"));
        assertEquals(0, provider.getTokenVersionFromAccessToken(token));
        assertThrows(JwtException.class, () -> provider.getUsernameFromRefreshToken(token));
    }

    @Test
    void refreshTokenContainsRefreshType() {
        JwtProvider provider = new JwtProvider(SECRET);
        ReflectionTestUtils.setField(provider, "accessExpire", 60000L);
        ReflectionTestUtils.setField(provider, "refreshExpire", 120000L);

        String token = provider.createRefreshToken("tester@example.com");

        assertEquals("tester@example.com", provider.getUsernameFromRefreshToken(token));
        assertThrows(JwtException.class, () -> provider.getUsernameFromAccessToken(token));
    }

    @Test
    void superAdminRoleReceivesAdminAccessPermission() {
        JwtProvider provider = new JwtProvider(SECRET);
        ReflectionTestUtils.setField(provider, "accessExpire", 60000L);
        ReflectionTestUtils.setField(provider, "refreshExpire", 120000L);

        String token = provider.createAccessToken("tester@example.com", "SUPER_ADMIN");

        assertEquals("SUPER_ADMIN", provider.getRolesFromAccessToken(token).get(0));
        assertTrue(provider.getPermissionsFromAccessToken(token).contains("ADMIN_ACCESS"));
    }

    @Test
    void accessTokenCanIncludeStoreOwnerRoleAndPermission() {
        JwtProvider provider = new JwtProvider(SECRET);
        ReflectionTestUtils.setField(provider, "accessExpire", 60000L);
        ReflectionTestUtils.setField(provider, "refreshExpire", 120000L);
        User user = User.builder()
                .username("owner@example.com")
                .role(PlateAuthorities.ROLE_USER)
                .tokenVersion(0)
                .build();

        String token = provider.createAccessToken(
                user,
                List.of(PlateAuthorities.ROLE_USER, PlateAuthorities.ROLE_STORE_OWNER),
                List.of(PlateAuthorities.PERMISSION_OWNER_ACCESS)
        );

        assertTrue(provider.getRolesFromAccessToken(token).contains(PlateAuthorities.ROLE_STORE_OWNER));
        assertTrue(provider.getPermissionsFromAccessToken(token).contains(PlateAuthorities.PERMISSION_OWNER_ACCESS));
    }

    @Test
    void validateReturnsFalseForMalformedToken() {
        JwtProvider provider = new JwtProvider(SECRET);
        ReflectionTestUtils.setField(provider, "accessExpire", 60000L);
        ReflectionTestUtils.setField(provider, "refreshExpire", 120000L);

        assertFalse(provider.validate("not-a-jwt"));
    }
}
