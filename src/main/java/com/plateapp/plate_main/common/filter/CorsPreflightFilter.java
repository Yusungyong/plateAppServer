package com.plateapp.plate_main.common.filter;

import com.plateapp.plate_main.auth.security.CorsProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class CorsPreflightFilter extends OncePerRequestFilter {

    private static final String ALLOWED_METHODS = "GET,POST,PUT,PATCH,DELETE,OPTIONS";
    private static final String ALLOWED_HEADERS = String.join(", ",
            HttpHeaders.AUTHORIZATION,
            HttpHeaders.CONTENT_TYPE,
            HttpHeaders.ACCEPT,
            HttpHeaders.ORIGIN,
            "X-Requested-With",
            "X-Request-Id"
    );
    private static final String EXPOSED_HEADERS = "X-Request-Id";

    private final CorsProperties properties;

    public CorsPreflightFilter(CorsProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!HttpMethod.OPTIONS.matches(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (origin != null && isAllowedOrigin(origin)) {
            response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
            response.setHeader(HttpHeaders.VARY, String.join(", ",
                    HttpHeaders.ORIGIN,
                    HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD,
                    HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS
            ));
            response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, ALLOWED_METHODS);
            response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, allowedHeaders(request));
            response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, EXPOSED_HEADERS);
            response.setHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE,
                    String.valueOf(properties.maxAge().toSeconds()));
            if (properties.allowCredentials()) {
                response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
            }
        }

        response.setStatus(HttpStatus.OK.value());
    }

    private boolean isAllowedOrigin(String origin) {
        List<String> allowedOrigins = properties.allowedOrigins();
        return allowedOrigins.contains("*") || allowedOrigins.contains(origin);
    }

    private String allowedHeaders(HttpServletRequest request) {
        String requestedHeaders = request.getHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
        if (requestedHeaders == null || requestedHeaders.isBlank()) {
            return ALLOWED_HEADERS;
        }
        return requestedHeaders;
    }
}
