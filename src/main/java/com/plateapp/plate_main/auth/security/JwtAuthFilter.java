// src/main/java/com/plateapp/plate_main/auth/security/JwtAuthFilter.java
package com.plateapp.plate_main.auth.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Locale;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    public static final String AUTH_ERROR_ATTR = "AUTH_ERROR";
    public static final String AUTH_ERROR_EXPIRED = "TOKEN_EXPIRED";
    public static final String AUTH_ERROR_INVALID = "TOKEN_INVALID";

    private static final String[] PUBLIC_PATHS = {
            "/auth/**",
            "/email/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/swagger-ui/index.html"
    };

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String path = request.getServletPath();
        for (String pattern : PUBLIC_PATHS) {
            if (PATH_MATCHER.match(pattern, path)) {
                return true;
            }
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
            SecurityContextHolder.clearContext();
            request.setAttribute(AUTH_ERROR_ATTR, AUTH_ERROR_INVALID);
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String username = jwtProvider.getUsernameFromAccessToken(token);
            String role = jwtProvider.getRoleFromAccessToken(token);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            username,
                            null,
                            Collections.singletonList(new SimpleGrantedAuthority(toAuthority(role)))
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
            return "ROLE_USER";
        }

        String normalized = role.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("ROLE_")) {
            return normalized;
        }

        return switch (normalized) {
            case "ADM", "ADMIN", "993" -> "ROLE_ADMIN";
            case "USR", "USER" -> "ROLE_USER";
            default -> "ROLE_" + normalized;
        };
    }
}
