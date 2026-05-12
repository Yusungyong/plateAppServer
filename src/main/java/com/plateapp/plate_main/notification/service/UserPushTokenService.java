package com.plateapp.plate_main.notification.service;

import com.plateapp.plate_main.auth.domain.User;
import com.plateapp.plate_main.notification.entity.Fp24UserPushToken;
import com.plateapp.plate_main.notification.repository.Fp24UserPushTokenRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserPushTokenService {

    private static final String LEGACY_DEVICE_PREFIX = "legacy:";
    private static final String LEGACY_PLATFORM = "LEGACY";

    private final Fp24UserPushTokenRepository tokenRepository;

    @Transactional(readOnly = true)
    public List<Fp24UserPushToken> findActiveTokens(Integer userId) {
        if (userId == null) {
            return List.of();
        }
        return tokenRepository.findByUserIdAndTokenStatusOrderByUpdatedAtDesc(userId, "ACTIVE");
    }

    @Transactional
    public Fp24UserPushToken upsertLegacyToken(User user, String pushToken) {
        if (user == null || user.getUserId() == null || pushToken == null || pushToken.isBlank()) {
            return null;
        }
        String deviceId = LEGACY_DEVICE_PREFIX + user.getUsername();
        Fp24UserPushToken token = tokenRepository.findByUserIdAndDeviceId(user.getUserId(), deviceId)
                .orElseGet(Fp24UserPushToken::new);
        token.setUserId(user.getUserId());
        token.setDeviceId(deviceId);
        token.setPlatform(LEGACY_PLATFORM);
        token.setPushToken(pushToken);
        token.setTokenStatus("ACTIVE");
        token.setLastSeenAt(LocalDateTime.now());
        return tokenRepository.save(token);
    }

    @Transactional
    public void invalidateToken(Fp24UserPushToken token) {
        if (token == null) {
            return;
        }
        token.setTokenStatus("INVALID");
        tokenRepository.save(token);
    }
}
