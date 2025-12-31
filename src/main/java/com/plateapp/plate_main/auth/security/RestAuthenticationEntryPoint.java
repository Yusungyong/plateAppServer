// src/main/java/com/plateapp/plate_main/auth/security/RestAuthenticationEntryPoint.java
package com.plateapp.plate_main.auth.security;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plateapp.plate_main.common.api.ApiResponse;
import com.plateapp.plate_main.common.error.ErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {

        Object flag = request.getAttribute(JwtAuthFilter.AUTH_ERROR_ATTR);

        ErrorCode ec;
        if (JwtAuthFilter.AUTH_ERROR_EXPIRED.equals(flag)) {
            ec = ErrorCode.AUTH_TOKEN_EXPIRED; // AUTH_402 (status는 401로 주는 걸 추천하지만 현재 enum 유지)
        } else {
            ec = ErrorCode.AUTH_UNAUTHORIZED;  // AUTH_401
        }

        response.setStatus(ec.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        objectMapper.writeValue(response.getWriter(),
                ApiResponse.fail(ec.getCode(), ec.getDefaultMessage()));
    }
}
