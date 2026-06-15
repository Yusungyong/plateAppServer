package com.plateapp.plate_main.auth.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.plateapp.plate_main.auth.domain.User;
import com.plateapp.plate_main.auth.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

class JwtAuthFilterTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsAccessTokenWhenStoredTokenVersionChanged() throws Exception {
        JwtProvider provider = provider();
        UserRepository userRepository = mock(UserRepository.class);
        JwtAuthFilter filter = new JwtAuthFilter(provider, userRepository);
        User issuedUser = User.builder()
                .username("admin@example.com")
                .role("ADMIN")
                .tokenVersion(1)
                .build();
        User currentUser = User.builder()
                .username("admin@example.com")
                .role("ADMIN")
                .tokenVersion(2)
                .build();
        String token = provider.createAccessToken(
                issuedUser,
                List.of(PlateAuthorities.PERMISSION_ADMIN_ACCESS)
        );
        when(userRepository.findById("admin@example.com")).thenReturn(Optional.of(currentUser));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/dashboard/summary");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);

        filter.doFilterInternal(request, new MockHttpServletResponse(), new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(
                JwtAuthFilter.AUTH_ERROR_INVALID,
                request.getAttribute(JwtAuthFilter.AUTH_ERROR_ATTR)
        );
    }

    private JwtProvider provider() {
        JwtProvider provider = new JwtProvider(SECRET);
        ReflectionTestUtils.setField(provider, "accessExpire", 60000L);
        ReflectionTestUtils.setField(provider, "refreshExpire", 120000L);
        return provider;
    }
}
