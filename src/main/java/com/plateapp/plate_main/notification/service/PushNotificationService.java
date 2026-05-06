package com.plateapp.plate_main.notification.service;

import com.plateapp.plate_main.auth.domain.User;
import com.plateapp.plate_main.auth.repository.UserRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotificationService {

    private final UserRepository userRepository;

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

        log.info(
                "Push dispatch requested user={} tokenPresent=true title={} body={} data={}",
                receiver.getUsername(),
                title,
                body,
                data
        );

        // TODO: Firebase Admin SDK integration point.
        return true;
    }
}
