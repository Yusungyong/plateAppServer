package com.plateapp.plate_main.notification.service;

import com.plateapp.plate_main.auth.domain.User;
import com.plateapp.plate_main.notification.entity.Fp24UserPushToken;
import com.plateapp.plate_main.notification.repository.Fp24UserPushTokenRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserPushTokenService {

    private static final String LEGACY_DEVICE_PREFIX = "legacy:";
    private static final String LEGACY_PLATFORM = "LEGACY";
    private static final String LOGIN_PLATFORM = "APP";

    private final Fp24UserPushTokenRepository tokenRepository;

    @Transactional(readOnly = true)
    public List<Fp24UserPushToken> findActiveTokens(Integer userId) {
        if (userId == null) {
            log.info("Find active push tokens skipped: userId null");
            return List.of();
        }
        List<Fp24UserPushToken> tokens = tokenRepository.findByUserIdAndTokenStatusOrderByUpdatedAtDesc(userId, "ACTIVE");
        log.info("Find active push tokens userId={} activeTokenCount={}", userId, tokens.size());
        return tokens;
    }

    @Transactional(readOnly = true)
    public String findLatestActiveTokenValue(Integer userId) {
        return findActiveTokens(userId).stream()
                .map(Fp24UserPushToken::getPushToken)
                .filter(token -> token != null && !token.isBlank())
                .findFirst()
                .orElse(null);
    }

    @Transactional
    public Fp24UserPushToken upsertLegacyToken(User user, String pushToken) {
        if (user == null || user.getUserId() == null || pushToken == null || pushToken.isBlank()) {
            log.info("Skip legacy push token upsert: invalid input userId={}", user == null ? null : user.getUserId());
            return null;
        }
        return upsertToken(user, LEGACY_DEVICE_PREFIX + user.getUsername(), LEGACY_PLATFORM, pushToken);
    }

    @Transactional
    public Fp24UserPushToken upsertLoginToken(User user, String deviceId, String pushToken) {
        return upsertAppToken(user, deviceId, LOGIN_PLATFORM, pushToken);
    }

    @Transactional
    public Fp24UserPushToken upsertAppToken(User user, String deviceId, String platform, String pushToken) {
        if (user == null || user.getUserId() == null || deviceId == null || deviceId.isBlank()
                || pushToken == null || pushToken.isBlank()) {
            log.info("Skip app push token upsert: invalid input userId={} deviceIdPresent={} tokenPresent={}",
                    user == null ? null : user.getUserId(),
                    deviceId != null && !deviceId.isBlank(),
                    pushToken != null && !pushToken.isBlank());
            return null;
        }
        String resolvedPlatform = (platform == null || platform.isBlank()) ? LOGIN_PLATFORM : platform.trim().toUpperCase();
        return upsertToken(user, deviceId, resolvedPlatform, pushToken);
    }

    @Transactional
    public void invalidateToken(Fp24UserPushToken token) {
        if (token == null) {
            return;
        }
        token.setTokenStatus("INVALID");
        Fp24UserPushToken saved = tokenRepository.save(token);
        log.info("Push token invalidated userId={} tokenId={}", saved.getUserId(), saved.getTokenId());
    }

    private Fp24UserPushToken upsertToken(User user, String deviceId, String platform, String pushToken) {
        Fp24UserPushToken token = tokenRepository.findByUserIdAndDeviceId(user.getUserId(), deviceId)
                .orElseGet(Fp24UserPushToken::new);
        token.setUserId(user.getUserId());
        token.setDeviceId(deviceId);
        token.setPlatform(platform);
        token.setPushToken(pushToken);
        token.setTokenStatus("ACTIVE");
        token.setLastSeenAt(LocalDateTime.now());
        Fp24UserPushToken saved = tokenRepository.save(token);
        log.info("Push token upserted userId={} tokenId={} deviceId={} platform={} status={}",
                saved.getUserId(), saved.getTokenId(), deviceId, platform, saved.getTokenStatus());
        return saved;
    }
}
