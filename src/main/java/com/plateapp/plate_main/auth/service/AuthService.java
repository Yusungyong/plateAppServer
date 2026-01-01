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
import com.plateapp.plate_main.auth.domain.Role;
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

    private static final String LOGIN_FAIL_MESSAGE = "아이디 또는 비밀번호가 올바르지 않습니다.";
    private static final String LOGIN_STATUS_SUCCESS = "SUCCESS";
    private static final String LOGIN_STATUS_FAIL = "FAIL";
    private static final String FAIL_REASON_USER_NOT_FOUND = "USER_NOT_FOUND";
    private static final String FAIL_REASON_PASSWORD_MISMATCH = "PASSWORD_MISMATCH";
    private static final String FAIL_REASON_PASSWORD_EMPTY = "PASSWORD_EMPTY";

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

        String email = normalizeEmail(request.getEmail());
        String rawPassword = request.getPassword();
        String nickname = normalizeValue(request.getNickname());

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
                .role(Role.USER.name())
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
        username = normalizeUsername(username);

        User user = userRepository.findById(username)
                .orElseThrow(() -> {
                    logLoginHistory(username, LOGIN_STATUS_FAIL, FAIL_REASON_USER_NOT_FOUND,
                            ipAddress, deviceId, deviceModel, os, osVersion, appVersion);
                    return new AuthException(ErrorCode.AUTH_UNAUTHORIZED, LOGIN_FAIL_MESSAGE);
                });

        if (password == null || password.isBlank()) {
            logLoginHistory(username, LOGIN_STATUS_FAIL, FAIL_REASON_PASSWORD_EMPTY,
                    ipAddress, deviceId, deviceModel, os, osVersion, appVersion);
            throw new AuthException(ErrorCode.AUTH_UNAUTHORIZED, LOGIN_FAIL_MESSAGE);
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            logLoginHistory(username, LOGIN_STATUS_FAIL, FAIL_REASON_PASSWORD_MISMATCH,
                    ipAddress, deviceId, deviceModel, os, osVersion, appVersion);
            throw new AuthException(ErrorCode.AUTH_UNAUTHORIZED, LOGIN_FAIL_MESSAGE);
        }

        // 토큰 생성
        String accessToken = jwtProvider.createAccessToken(username);
        String refreshToken = jwtProvider.createRefreshToken(username);

        // 멀티 디바이스: deviceId가 있으면 해당 디바이스 것만 삭제, 없으면 전체 삭제
        if (deviceId != null && !deviceId.isBlank()) {
            refreshTokenRepository.deleteByUsernameAndDeviceId(username, deviceId);
        } else {
            refreshTokenRepository.deleteByUsername(username);
        }

        persistRefreshToken(username, refreshToken, deviceId);

        // 성공 로그
        logLoginHistory(username, LOGIN_STATUS_SUCCESS, null,
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
        if (isExpired(saved.getExpiryDate())) {
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

        saved.setRefreshToken(newRefresh);
        saved.setExpiryDate(toExpiry(newRefresh));
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
                    .loginDatetime(nowUtc())
                    .ipAddress(ipAddress)
                    .loginStatus(status)
                    .failReason(failReason)
                    .deviceId(deviceId)
                    .deviceModel(deviceModel)
                    .os(os)
                    .osVersion(osVersion)
                    .appVersion(appVersion)
                    .createdAt(nowUtc())
                    .build();

            loginHistoryRepository.save(history);
        } catch (Exception e) {
            // 히스토리 적재 실패했다고 로그인 자체가 깨지면 안 되므로 로그만 남김
            log.warn("Failed to log login history for {}: {}", username, e.getMessage());
        }
    }

    private void persistRefreshToken(String username, String refreshToken, String deviceId) {
        refreshTokenRepository.save(
                RefreshToken.builder()
                        .username(username)
                        .refreshToken(refreshToken)
                        .expiryDate(toExpiry(refreshToken))
                        .deviceId(normalizeValue(deviceId))
                        .createdAt(nowUtc())
                        .build()
        );
    }

    private OffsetDateTime toExpiry(String refreshToken) {
        Date refreshExpDate = jwtProvider.getExpiration(refreshToken);
        return refreshExpDate.toInstant().atOffset(ZoneOffset.UTC);
    }

    private OffsetDateTime nowUtc() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    private boolean isExpired(OffsetDateTime expiryDate) {
        return expiryDate.isBefore(nowUtc());
    }

    private String normalizeUsername(String username) {
        return normalizeValue(username).toLowerCase();
    }

    private String normalizeEmail(String email) {
        return normalizeValue(email).toLowerCase();
    }

    private String normalizeValue(String value) {
        return value == null ? "" : value.trim();
    }

    public record AuthTokens(String accessToken, String refreshToken) {}
}
