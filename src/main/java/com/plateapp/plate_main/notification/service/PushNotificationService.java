package com.plateapp.plate_main.notification.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.plateapp.plate_main.auth.domain.User;
import com.plateapp.plate_main.auth.repository.UserRepository;
import com.plateapp.plate_main.notification.entity.Fp24UserPushToken;
import com.plateapp.plate_main.notification.entity.Fp25PushDeliveryLog;
import com.plateapp.plate_main.notification.repository.Fp25PushDeliveryLogRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotificationService {

    private final UserRepository userRepository;
    private final UserPushTokenService userPushTokenService;
    private final Fp25PushDeliveryLogRepository deliveryLogRepository;
    private final ObjectProvider<FirebaseMessaging> firebaseMessagingProvider;

    public boolean sendToUsername(String receiverUsername, Long notificationId, String title, String body, Map<String, String> data) {
        return userRepository.findById(receiverUsername)
                .map(user -> sendToUser(user, notificationId, title, body, data))
                .orElse(false);
    }

    public boolean sendToUser(User receiver, Long notificationId, String title, String body, Map<String, String> data) {
        if (receiver == null || receiver.getUsername() == null || receiver.getUsername().isBlank() || receiver.getUserId() == null) {
            return false;
        }

        FirebaseMessaging firebaseMessaging = firebaseMessagingProvider.getIfAvailable();
        if (firebaseMessaging == null) {
            log.info("Skip push send: FirebaseMessaging bean unavailable for user={}", receiver.getUsername());
            return false;
        }

        List<Fp24UserPushToken> activeTokens = new ArrayList<>(userPushTokenService.findActiveTokens(receiver.getUserId()));
        if (activeTokens.isEmpty()) {
            log.debug("Skip push send: no active token for user={}", receiver.getUsername());
            return false;
        }

        boolean sent = false;
        for (Fp24UserPushToken token : activeTokens) {
            sent |= sendToToken(receiver, token, notificationId, title, body, data, firebaseMessaging);
        }
        return sent;
    }

    private boolean sendToToken(
            User receiver,
            Fp24UserPushToken token,
            Long notificationId,
            String title,
            String body,
            Map<String, String> data,
            FirebaseMessaging firebaseMessaging
    ) {
        Message message = Message.builder()
                .setToken(token.getPushToken())
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putAllData(data)
                .build();

        try {
            String messageId = firebaseMessaging.send(message);
            saveDeliveryLog(token, notificationId, "SUCCESS", messageId, null, null);
            log.info("Push sent user={} tokenId={} messageId={} data={}",
                    receiver.getUsername(), token.getTokenId(), messageId, data);
            return true;
        } catch (FirebaseMessagingException e) {
            saveDeliveryLog(
                    token,
                    notificationId,
                    "FAILED",
                    null,
                    e.getMessagingErrorCode() == null ? null : e.getMessagingErrorCode().name(),
                    e.getMessage()
            );
            log.warn("Push send failed user={} tokenId={} errorCode={} message={}",
                    receiver.getUsername(),
                    token.getTokenId(),
                    e.getMessagingErrorCode(),
                    e.getMessage());
            if (shouldInvalidateToken(e)) {
                userPushTokenService.invalidateToken(token);
            }
            return false;
        }
    }

    private void saveDeliveryLog(
            Fp24UserPushToken token,
            Long notificationId,
            String status,
            String providerMessageId,
            String errorCode,
            String errorMessage
    ) {
        Fp25PushDeliveryLog log = new Fp25PushDeliveryLog();
        log.setNotificationId(notificationId);
        log.setTokenId(token.getTokenId());
        log.setProvider("FCM");
        log.setProviderMessageId(providerMessageId);
        log.setDeliveryStatus(status);
        log.setErrorCode(errorCode);
        log.setErrorMessage(errorMessage);
        deliveryLogRepository.save(log);
    }

    private boolean shouldInvalidateToken(FirebaseMessagingException e) {
        return e.getMessagingErrorCode() != null
                && switch (e.getMessagingErrorCode()) {
                    case UNREGISTERED, INVALID_ARGUMENT -> true;
                    default -> false;
                };
    }
}
