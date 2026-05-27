package com.plateapp.plate_main.notification.service;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDispatchAsyncService {

    private final PushNotificationService pushNotificationService;

    @Async("notificationTaskExecutor")
    public void sendPushAsync(
            String receiverUsername,
            Integer receiverUserId,
            Long eventId,
            Long notificationId,
            String eventType,
            String title,
            String message,
            Map<String, String> data
    ) {
        log.info("Async push dispatch started eventId={} notificationId={} eventType={} receiverUserId={}",
                eventId, notificationId, eventType, receiverUserId);
        try {
            boolean pushSent = pushNotificationService.sendToUsername(receiverUsername, notificationId, title, message, data);
            log.info("Async push dispatch completed eventId={} notificationId={} eventType={} receiverUserId={} pushSent={}",
                    eventId, notificationId, eventType, receiverUserId, pushSent);
        } catch (RuntimeException e) {
            log.warn("Async push dispatch failed eventId={} notificationId={} eventType={} receiverUserId={}",
                    eventId, notificationId, eventType, receiverUserId, e);
        }
    }
}
