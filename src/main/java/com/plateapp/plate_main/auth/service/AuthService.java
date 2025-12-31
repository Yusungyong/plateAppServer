// src/main/java/com/plateapp/plate_main/auth/service/AuthService.java
package com.plateapp.plate_main.auth.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plateapp.plate_main.auth.domain.LoginHistory;
import com.plateapp.plate_main.auth.domain.RefreshToken;
import com.plateapp.plate_main.auth.domain.User;
import com.plateapp.plate_main.auth.dto.SignupRequest;
import com.plateapp.plate_main.auth.exception.AuthException;
import com.plateapp.plate_main.auth.repository.LoginHistoryRepository;
import com.plateapp.plate_main.auth.repository.RefreshTokenRepository;
import com.plateapp.plate_main.auth.repository.UserRepository;
import com.plateapp.plate_main.auth.security.JwtProvider;
import com.plateapp.plate_main.common.error.ErrorCode;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final JwtProvider jwtProvider;
    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * 회원가입
     */
    @Transactional
    public void signup(SignupRequest request) {

        String email = request.getEmail() == null ? "" : request.getEmail().trim();
        String rawPassword = request.getPassword();
        String nickname = request.getNickname() == null ? "" : request.getNickname().trim();

        // 이미 가입된 이메일인지 체크
        if (userRepository.existsByEmail(email)) {
            throw new AuthException(ErrorCode.COMMON_CONFLICT, "이미 가입된 이메일입니다.");
        }

        LocalDate today = LocalDate.now(); // username = email 전략

        User user = User.builder()
                .username(email)
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .nickname(nickname)
                .createdAt(today)
                .updatedAt(today)
                .build();

        userRepository.save(user);
    }

    /**
     * 로그인 (멀티 디바이스 + 로그인 이력 기록)
     */
    @Transactional
    public AuthTokens login(
            String username,
            String password,
            String deviceId,
            String deviceModel,
            String os,
            String osVersion,
            String appVersion,
            String ipAddress
    ) {

        // ✅ 보안상 "계정 존재 여부"가 드러나지 않게 메시지 통일 권장
        final String loginFailMessage = "아이디 또는 비밀번호가 올바르지 않습니다.";

        User user = userRepository.findById(username)
                .orElseThrow(() -> {
                    logLoginHistory(username, "FAIL", "USER_NOT_FOUND",
                            ipAddress, deviceId, deviceModel, os, osVersion, appVersion);
                    return new AuthException(ErrorCode.AUTH_UNAUTHORIZED, loginFailMessage);
                });

        if (!passwordEncoder.matches(password, user.getPassword())) {
            logLoginHistory(username, "FAIL", "PASSWORD_MISMATCH",
                    ipAddress, deviceId, deviceModel, os, osVersion, appVersion);
            throw new AuthException(ErrorCode.AUTH_UNAUTHORIZED, loginFailMessage);
        }

        // 토큰 생성
        String accessToken = jwtProvider.createAccessToken(username);
        String refreshToken = jwtProvider.createRefreshToken(username);

        // Refresh TTL을 JWT와 맞춤
        Date refreshExpDate = jwtProvider.getExpiration(refreshToken);
        OffsetDateTime refreshExpiry = refreshExpDate.toInstant().atOffset(ZoneOffset.UTC);

        // 멀티 디바이스: deviceId가 있으면 해당 디바이스 것만 삭제, 없으면 전체 삭제
        if (deviceId != null && !deviceId.isBlank()) {
            refreshTokenRepository.deleteByUsernameAndDeviceId(username, deviceId);
        } else {
            refreshTokenRepository.deleteByUsername(username);
        }

        refreshTokenRepository.save(
                RefreshToken.builder()
                        .username(username)
                        .refreshToken(refreshToken)
                        .expiryDate(refreshExpiry)
                        .deviceId(deviceId)
                        .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                        .build()
        );

        // 성공 로그
        logLoginHistory(username, "SUCCESS", null,
                ipAddress, deviceId, deviceModel, os, osVersion, appVersion);

        log.debug("AccessToken issued for {}.", username);

        return new AuthTokens(accessToken, refreshToken);
    }

    /**
     * Refresh Token으로 재발급 (TTL 동기화 + refresh 타입 검증)
     */
    @Transactional
    public AuthTokens refresh(String refreshToken) {

        // 1) DB에 존재하는지 확인 (회전 토큰: 최신 토큰만 유효하게 유지)
        RefreshToken saved = refreshTokenRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new AuthException(ErrorCode.AUTH_REFRESH_INVALID));

        // 2) DB TTL 체크 (만료면 삭제하고 종료)
        if (saved.getExpiryDate().isBefore(OffsetDateTime.now(ZoneOffset.UTC))) {
            refreshTokenRepository.delete(saved);
            throw new AuthException(ErrorCode.AUTH_REFRESH_EXPIRED);
        }

        // 3) JWT 자체 검증 (서명/만료/typ=refresh)
        final String usernameFromJwt;
        try {
            usernameFromJwt = jwtProvider.getUsernameFromRefreshToken(refreshToken);
        } catch (ExpiredJwtException e) {
            refreshTokenRepository.delete(saved);
            throw new AuthException(ErrorCode.AUTH_REFRESH_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            refreshTokenRepository.delete(saved);
            throw new AuthException(ErrorCode.AUTH_REFRESH_INVALID);
        }

        // 4) DB에 저장된 username과 JWT subject가 일치하는지 검증
        String username = saved.getUsername();
        if (!username.equals(usernameFromJwt)) {
            refreshTokenRepository.delete(saved);
            throw new AuthException(ErrorCode.AUTH_REFRESH_INVALID);
        }

        // 5) 새 토큰 발급 (rotation)
        String newAccess = jwtProvider.createAccessToken(username);
        String newRefresh = jwtProvider.createRefreshToken(username);

        Date refreshExpDate = jwtProvider.getExpiration(newRefresh);
        OffsetDateTime refreshExpiry = refreshExpDate.toInstant().atOffset(ZoneOffset.UTC);

        saved.setRefreshToken(newRefresh);
        saved.setExpiryDate(refreshExpiry);
        refreshTokenRepository.save(saved);

        return new AuthTokens(newAccess, newRefresh);
    }

    /**
     * 로그인 이력 기록 (fp_105)
     */
    private void logLoginHistory(
            String username,
            String status,
            String failReason,
            String ipAddress,
            String deviceId,
            String deviceModel,
            String os,
            String osVersion,
            String appVersion
    ) {
        try {
            LoginHistory history = LoginHistory.builder()
                    .username(username)
                    .loginDatetime(OffsetDateTime.now(ZoneOffset.UTC))
                    .ipAddress(ipAddress)
                    .loginStatus(status)
                    .failReason(failReason)
                    .deviceId(deviceId)
                    .deviceModel(deviceModel)
                    .os(os)
                    .osVersion(osVersion)
                    .appVersion(appVersion)
                    .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                    .build();

            loginHistoryRepository.save(history);
        } catch (Exception e) {
            // 히스토리 적재 실패했다고 로그인 자체가 깨지면 안 되므로 로그만 남김
            log.warn("Failed to log login history for {}: {}", username, e.getMessage());
        }
    }

    public record AuthTokens(String accessToken, String refreshToken) {}
}
