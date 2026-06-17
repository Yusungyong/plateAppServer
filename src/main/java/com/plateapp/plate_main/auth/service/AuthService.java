package com.plateapp.plate_main.auth.service;

import com.plateapp.plate_main.auth.domain.RefreshToken;
import com.plateapp.plate_main.auth.domain.User;
import com.plateapp.plate_main.auth.dto.SignupRequest;
import com.plateapp.plate_main.auth.exception.AuthException;
import com.plateapp.plate_main.auth.repository.RefreshTokenRepository;
import com.plateapp.plate_main.auth.repository.UserRepository;
import com.plateapp.plate_main.auth.security.PlateAuthorities;
import com.plateapp.plate_main.auth.security.JwtProvider;
import com.plateapp.plate_main.auth.security.RefreshTokenHasher;
import com.plateapp.plate_main.common.error.ErrorCode;
import com.plateapp.plate_main.notification.service.UserPushTokenService;
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
    private final UserPushTokenService userPushTokenService;
    private final AdminPermissionService adminPermissionService;

    @Transactional
    public void signup(SignupRequest request) {
        String username = request.getUsername() == null ? "" : request.getUsername().trim();
        String email = request.getEmail() == null ? "" : request.getEmail().trim();
        String rawPassword = request.getPassword();
        String nickname = request.getNickname() == null ? "" : request.getNickname().trim();

        if (userRepository.existsById(username)) {
            throw new AuthException(ErrorCode.COMMON_CONFLICT, "이미 사용 중인 로그인 ID입니다.");
        }
        if (userRepository.existsByEmail(email)) {
            throw new AuthException(ErrorCode.COMMON_CONFLICT, "이미 가입된 이메일입니다.");
        }

        LocalDate today = LocalDate.now();

        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .nickname(nickname)
                .createdAt(today)
                .updatedAt(today)
                .build();

        userRepository.saveAndFlush(user);
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
            String fcmToken,
            String ipAddress
    ) {
        boolean failLogged = false;
        final String loginFailMessage = "아이디 또는 비밀번호가 올바르지 않습니다.";

        try {
            User user = userRepository.findById(username).orElse(null);
            if (user == null) {
                loginHistoryService.log(username, null, "FAIL", "USER_NOT_FOUND",
                        ipAddress, deviceId, deviceModel, os, osVersion, appVersion);
                failLogged = true;
                throw new AuthException(ErrorCode.AUTH_UNAUTHORIZED, loginFailMessage);
            }

            if (!passwordEncoder.matches(password, user.getPassword())) {
                loginHistoryService.log(username, user.getUserId(), "FAIL", "PASSWORD_MISMATCH",
                        ipAddress, deviceId, deviceModel, os, osVersion, appVersion);
                failLogged = true;
                throw new AuthException(ErrorCode.AUTH_UNAUTHORIZED, loginFailMessage);
            }

            String accessToken = jwtProvider.createAccessToken(
                    user,
                    adminPermissionService.resolveRoles(user),
                    adminPermissionService.resolvePermissions(user)
            );
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
                            .userId(user.getUserId())
                            .refreshToken(RefreshTokenHasher.sha256(refreshToken))
                            .expiryDate(refreshExpiry)
                            .deviceId(deviceId)
                            .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                            .build()
            );

            loginHistoryService.log(username, user.getUserId(), "SUCCESS", null,
                    ipAddress, deviceId, deviceModel, os, osVersion, appVersion);
            userPushTokenService.upsertLoginToken(user, deviceId, fcmToken);

            log.debug("AccessToken issued for {}.", username);
            return new AuthTokens(accessToken, refreshToken);
        } catch (AuthException e) {
            if (!failLogged) {
                loginHistoryService.log(username, null, "FAIL", e.getErrorCode().name(),
                        ipAddress, deviceId, deviceModel, os, osVersion, appVersion);
            }
            throw e;
        } catch (Exception e) {
            loginHistoryService.log(username, null, "FAIL", "UNEXPECTED_ERROR",
                    ipAddress, deviceId, deviceModel, os, osVersion, appVersion);
            throw e;
        }
    }

    @Transactional
    public AuthTokens refresh(String refreshToken) {

        String refreshTokenHash = RefreshTokenHasher.sha256(refreshToken);
        RefreshToken saved = refreshTokenRepository.findByRefreshToken(refreshTokenHash)
                .or(() -> refreshTokenRepository.findByRefreshToken(refreshToken))
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

        User user = userRepository.findById(username)
                .orElseThrow(() -> new AuthException(ErrorCode.USER_NOT_FOUND));

        String newAccess = jwtProvider.createAccessToken(
                user,
                adminPermissionService.resolveRoles(user),
                adminPermissionService.resolvePermissions(user)
        );
        String newRefresh = jwtProvider.createRefreshToken(username);

        Date refreshExpDate = jwtProvider.getExpiration(newRefresh);
        OffsetDateTime refreshExpiry = refreshExpDate.toInstant().atOffset(ZoneOffset.UTC);

        saved.setRefreshToken(RefreshTokenHasher.sha256(newRefresh));
        saved.setExpiryDate(refreshExpiry);
        saved.setUserId(user.getUserId());
        refreshTokenRepository.save(saved);

        return new AuthTokens(newAccess, newRefresh);
    }

    public record AuthTokens(String accessToken, String refreshToken) {}

    private String normalizeRole(String role) {
        return PlateAuthorities.toRole(role);
    }
}
