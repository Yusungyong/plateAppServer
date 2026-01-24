package com.plateapp.plate_main.auth.service;

import com.plateapp.plate_main.auth.domain.RefreshToken;
import com.plateapp.plate_main.auth.domain.User;
import com.plateapp.plate_main.auth.dto.SignupRequest;
import com.plateapp.plate_main.auth.exception.AuthException;
import com.plateapp.plate_main.auth.repository.RefreshTokenRepository;
import com.plateapp.plate_main.auth.repository.UserRepository;
import com.plateapp.plate_main.auth.security.JwtProvider;
import com.plateapp.plate_main.common.error.ErrorCode;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final LoginHistoryService loginHistoryService;
    private final JwtProvider jwtProvider;
    private final BCryptPasswordEncoder passwordEncoder;

    @Transactional
    public void signup(SignupRequest request) {
        String email = request.getEmail() == null ? "" : request.getEmail().trim();
        String rawPassword = request.getPassword();
        String nickname = request.getNickname() == null ? "" : request.getNickname().trim();

        if (userRepository.existsByEmail(email)) {
            throw new AuthException(ErrorCode.COMMON_CONFLICT, "이미 가입된 이메일입니다.");
        }

        LocalDate today = LocalDate.now();

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
        boolean failLogged = false;
        final String loginFailMessage = "아이디 또는 비밀번호가 올바르지 않습니다.";

        try {
            User user = userRepository.findById(username).orElse(null);
            if (user == null) {
                loginHistoryService.log(username, "FAIL", "USER_NOT_FOUND",
                        ipAddress, deviceId, deviceModel, os, osVersion, appVersion);
                failLogged = true;
                throw new AuthException(ErrorCode.AUTH_UNAUTHORIZED, loginFailMessage);
            }

            if (!passwordEncoder.matches(password, user.getPassword())) {
                loginHistoryService.log(username, "FAIL", "PASSWORD_MISMATCH",
                        ipAddress, deviceId, deviceModel, os, osVersion, appVersion);
                failLogged = true;
                throw new AuthException(ErrorCode.AUTH_UNAUTHORIZED, loginFailMessage);
            }

            String accessToken = jwtProvider.createAccessToken(username);
            String refreshToken = jwtProvider.createRefreshToken(username);

            Date refreshExpDate = jwtProvider.getExpiration(refreshToken);
            OffsetDateTime refreshExpiry = refreshExpDate.toInstant().atOffset(ZoneOffset.UTC);

            refreshTokenRepository.deleteByUsername(username);
            if (deviceId != null && !deviceId.isBlank()) {
                refreshTokenRepository.deleteByUsernameAndDeviceId(username, deviceId);
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

            loginHistoryService.log(username, "SUCCESS", null,
                    ipAddress, deviceId, deviceModel, os, osVersion, appVersion);

            log.debug("AccessToken issued for {}.", username);
            return new AuthTokens(accessToken, refreshToken);
        } catch (AuthException e) {
            if (!failLogged) {
                loginHistoryService.log(username, "FAIL", e.getErrorCode().name(),
                        ipAddress, deviceId, deviceModel, os, osVersion, appVersion);
            }
            throw e;
        } catch (Exception e) {
            loginHistoryService.log(username, "FAIL", "UNEXPECTED_ERROR",
                    ipAddress, deviceId, deviceModel, os, osVersion, appVersion);
            throw e;
        }
    }

    @Transactional
    public AuthTokens refresh(String refreshToken) {

        RefreshToken saved = refreshTokenRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new AuthException(ErrorCode.AUTH_REFRESH_INVALID));

        if (saved.getExpiryDate().isBefore(OffsetDateTime.now(ZoneOffset.UTC))) {
            refreshTokenRepository.delete(saved);
            throw new AuthException(ErrorCode.AUTH_REFRESH_EXPIRED);
        }

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

        String username = saved.getUsername();
        if (!username.equals(usernameFromJwt)) {
            refreshTokenRepository.delete(saved);
            throw new AuthException(ErrorCode.AUTH_REFRESH_INVALID);
        }

        String newAccess = jwtProvider.createAccessToken(username);
        String newRefresh = jwtProvider.createRefreshToken(username);

        Date refreshExpDate = jwtProvider.getExpiration(newRefresh);
        OffsetDateTime refreshExpiry = refreshExpDate.toInstant().atOffset(ZoneOffset.UTC);

        saved.setRefreshToken(newRefresh);
        saved.setExpiryDate(refreshExpiry);
        refreshTokenRepository.save(saved);

        return new AuthTokens(newAccess, newRefresh);
    }

    public record AuthTokens(String accessToken, String refreshToken) {}
}
