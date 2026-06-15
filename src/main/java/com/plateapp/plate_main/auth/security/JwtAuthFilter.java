package com.plateapp.plate_main.auth.security;

import com.plateapp.plate_main.auth.repository.UserRepository;
import com.plateapp.plate_main.auth.domain.User;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    public static final String AUTH_ERROR_ATTR = "AUTH_ERROR";
    public static final String AUTH_ERROR_EXPIRED = "TOKEN_EXPIRED";
    public static final String AUTH_ERROR_INVALID = "TOKEN_INVALID";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String path = request.getServletPath();
        for (String pattern : SecurityPaths.PUBLIC_MATCHERS) {
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
            List<String> roles = jwtProvider.getRolesFromAccessToken(token);
            List<String> permissions = jwtProvider.getPermissionsFromAccessToken(token);

            User user = userRepository.findById(username).orElse(null);
            int tokenVersion = jwtProvider.getTokenVersionFromAccessToken(token);
            if (user == null || tokenVersion != normalizeTokenVersion(user.getTokenVersion())) {
                SecurityContextHolder.clearContext();
                request.setAttribute(AUTH_ERROR_ATTR, AUTH_ERROR_INVALID);
                filterChain.doFilter(request, response);
                return;
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            username,
                            null,
                            PlateAuthorities.toSpringAuthorities(roles, permissions).stream()
                                    .map(SimpleGrantedAuthority::new)
                                    .toList()
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

    private int normalizeTokenVersion(Integer value) {
        return value == null ? 0 : value;
    }
}
