// src/main/java/com/plateapp/plate_main/auth/security/RestAccessDeniedHandler.java
package com.plateapp.plate_main.auth.security;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plateapp.plate_main.common.api.ApiResponse;
import com.plateapp.plate_main.common.error.ErrorCode;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {

        ErrorCode ec = ErrorCode.AUTH_FORBIDDEN;

        response.setStatus(ec.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Void> body = ApiResponse.fail(ec.getCode(), ec.getDefaultMessage());
        objectMapper.writeValue(response.getWriter(), body);
    }
}
