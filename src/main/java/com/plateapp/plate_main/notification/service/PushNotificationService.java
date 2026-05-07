package com.plateapp.plate_main.notification.service;

import com.plateapp.plate_main.auth.domain.User;
import com.plateapp.plate_main.auth.repository.UserRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
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
    private final ObjectProvider<FirebaseMessaging> firebaseMessagingProvider;

    public boolean sendToUsername(String receiverUsername, String title, String body, Map<String, String> data) {
        return userRepository.findById(receiverUsername)
                .map(user -> sendToUser(user, title, body, data))
                .orElse(false);
    }

    public boolean sendToUser(User receiver, String title, String body, Map<String, String> data) {
        if (receiver == null || receiver.getUsername() == null || receiver.getUsername().isBlank()) {
            return false;
        }
        if (receiver.getFcmToken() == null || receiver.getFcmToken().isBlank()) {
            log.debug("Skip push send: no fcm token for user={}", receiver.getUsername());
            return false;
        }

        FirebaseMessaging firebaseMessaging = firebaseMessagingProvider.getIfAvailable();
        if (firebaseMessaging == null) {
            log.info("Skip push send: FirebaseMessaging bean unavailable for user={}", receiver.getUsername());
            return false;
        }

        Message message = Message.builder()
                .setToken(receiver.getFcmToken())
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .putAllData(data)
                .build();

        try {
            String messageId = firebaseMessaging.send(message);
            log.info("Push sent user={} messageId={} data={}", receiver.getUsername(), messageId, data);
            return true;
        } catch (FirebaseMessagingException e) {
            log.warn("Push send failed user={} errorCode={} message={}",
                    receiver.getUsername(),
                    e.getMessagingErrorCode(),
                    e.getMessage());
            if (shouldInvalidateToken(e)) {
                receiver.setFcmToken(null);
                userRepository.save(receiver);
                log.info("Invalidated fcm token for user={}", receiver.getUsername());
            }
            return false;
        }
    }

    private boolean shouldInvalidateToken(FirebaseMessagingException e) {
        return e.getMessagingErrorCode() != null
                && switch (e.getMessagingErrorCode()) {
                    case UNREGISTERED, INVALID_ARGUMENT -> true;
                    default -> false;
                };
    }
}
