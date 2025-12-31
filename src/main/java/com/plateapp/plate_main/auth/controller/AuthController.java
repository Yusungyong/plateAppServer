// src/main/java/com/plateapp/plate_main/auth/controller/AuthController.java
package com.plateapp.plate_main.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.plateapp.plate_main.auth.dto.AppleLoginRequest;
import com.plateapp.plate_main.auth.dto.GoogleLoginRequest;
import com.plateapp.plate_main.auth.dto.KakaoLoginRequest;
import com.plateapp.plate_main.auth.dto.SignupRequest;
import com.plateapp.plate_main.auth.dto.TokenResponse;
import com.plateapp.plate_main.auth.service.AuthService;
import com.plateapp.plate_main.auth.service.AuthService.AuthTokens;
import com.plateapp.plate_main.auth.service.SocialAuthService;
import com.plateapp.plate_main.common.api.ApiResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final SocialAuthService socialAuthService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> signup(@Valid @RequestBody SignupRequest req) {
        authService.signup(req);
        return ResponseEntity.ok(ApiResponse.ok(null, "회원가입 완료"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthTokens>> login(
            @Valid @RequestBody LoginRequest req,
            HttpServletRequest httpRequest
    ) {
        String ip = extractClientIp(httpRequest);

        AuthTokens tokens = authService.login(
                req.username(),
                req.password(),
                req.deviceId(),
                req.deviceModel(),
                req.os(),
                req.osVersion(),
                req.appVersion(),
                ip
        );

        return ResponseEntity.ok(ApiResponse.ok(tokens));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthTokens>> refresh(@Valid @RequestBody RefreshRequest req) {
        AuthTokens tokens = authService.refresh(req.refreshToken());
        return ResponseEntity.ok(ApiResponse.ok(tokens));
    }

    // ===== Social Login (중복 제거: provider별로 명확히) =====

    @PostMapping("/social/apple")
    public ResponseEntity<ApiResponse<TokenResponse>> appleLogin(
            @Valid @RequestBody AppleLoginRequest request
    ) {
        TokenResponse tokenResponse = socialAuthService.loginWithApple(request);
        return ResponseEntity.ok(ApiResponse.ok(tokenResponse));
    }

    @PostMapping("/social/kakao")
    public ResponseEntity<ApiResponse<TokenResponse>> kakaoLogin(
            @Valid @RequestBody KakaoLoginRequest request
    ) {
        TokenResponse tokenResponse = socialAuthService.loginWithKakao(request);
        return ResponseEntity.ok(ApiResponse.ok(tokenResponse));
    }

    @PostMapping("/social/google")
    public ResponseEntity<ApiResponse<TokenResponse>> googleLogin(
            @Valid @RequestBody GoogleLoginRequest request
    ) {
        TokenResponse tokenResponse = socialAuthService.loginWithGoogle(request);
        return ResponseEntity.ok(ApiResponse.ok(tokenResponse));
    }

    // ===== helpers =====

    private String extractClientIp(HttpServletRequest request) {
        String[] headers = {
                "X-Forwarded-For",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR"
        };
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    // ===== Request DTOs (record + validation) =====

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password,
            String deviceId,
            String deviceModel,
            String os,
            String osVersion,
            String appVersion
    ) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}
}
