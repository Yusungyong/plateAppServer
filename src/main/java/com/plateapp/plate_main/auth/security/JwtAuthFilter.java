// src/main/java/com/plateapp/plate_main/auth/security/JwtAuthFilter.java
package com.plateapp.plate_main.auth.security;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import com.plateapp.plate_main.auth.domain.Role;
import com.plateapp.plate_main.auth.domain.User;
import com.plateapp.plate_main.auth.repository.UserRepository;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    public static final String AUTH_ERROR_ATTR = "AUTH_ERROR";
    public static final String AUTH_ERROR_EXPIRED = "TOKEN_EXPIRED";
    public static final String AUTH_ERROR_INVALID = "TOKEN_INVALID";

    private static final String ROLE_PREFIX = "ROLE_";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // ✅ 프리플라이트는 JWT 로직을 타지 않게 (깔끔)
        if (HttpMethod.OPTIONS.matches(request.getMethod())) return true;

        String path = request.getServletPath();
        for (String pattern : SecurityPaths.PUBLIC_MATCHERS) {
            if (PATH_MATCHER.match(pattern, path)) return true;
        }
        return false;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String auth = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (auth == null || !auth.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = auth.substring(7).trim();
        if (token.isEmpty()) {
            // 빈 토큰은 무효로 처리
            SecurityContextHolder.clearContext();
            request.setAttribute(AUTH_ERROR_ATTR, AUTH_ERROR_INVALID);
            filterChain.doFilter(request, response);
            return;
        }

        try {
        	String username = jwtProvider.getUsernameFromAccessToken(token);

            Optional<User> user = userRepository.findById(username);
            if (user.isEmpty()) {
                SecurityContextHolder.clearContext();
                request.setAttribute(AUTH_ERROR_ATTR, AUTH_ERROR_INVALID);
                filterChain.doFilter(request, response);
                return;
            }

            String authority = toAuthority(user.get().getRole());
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            username,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority(authority))
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (ExpiredJwtException e) {
            SecurityContextHolder.clearContext();
            request.setAttribute(AUTH_ERROR_ATTR, AUTH_ERROR_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            SecurityContextHolder.clearContext();
            request.setAttribute(AUTH_ERROR_ATTR, AUTH_ERROR_INVALID);
        }

        filterChain.doFilter(request, response);
    }

    private String toAuthority(String role) {
        if (role == null || role.isBlank()) {
            return ROLE_PREFIX + Role.USER.name();
        }
        String normalized = role.trim().toUpperCase();
        if (normalized.startsWith(ROLE_PREFIX)) {
            return normalized;
        }
        return ROLE_PREFIX + normalized;
    }
}
