package com.plateapp.plate_main.notification.service;

import com.plateapp.plate_main.auth.domain.User;
import com.plateapp.plate_main.auth.repository.UserRepository;
import com.plateapp.plate_main.notification.entity.Fp20Notification;
import com.plateapp.plate_main.notification.repository.NotificationRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationCommandService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final PushNotificationService pushNotificationService;

    @Transactional
    public void notifyStoreLike(String actorUsername, String receiverUsername, Integer storeId) {
        createAndDispatch(actorUsername, receiverUsername, "LIKE", "New like", actorUsername + " liked your store.", "store", toLong(storeId), null, null);
    }

    @Transactional
    public void notifyStoreComment(String actorUsername, String receiverUsername, Integer storeId, Integer commentId) {
        createAndDispatch(actorUsername, receiverUsername, "COMMENT", "New comment", actorUsername + " commented on your store.", "store", toLong(storeId), toLong(commentId), null);
    }

    @Transactional
    public void notifyStoreReply(String actorUsername, String receiverUsername, Integer storeId, Integer commentId, Integer replyId) {
        createAndDispatch(actorUsername, receiverUsername, "REPLY", "New reply", actorUsername + " replied to your comment.", "store", toLong(storeId), toLong(commentId), toLong(replyId));
    }

    @Transactional
    public void notifyFriendRequest(String actorUsername, String receiverUsername, Integer requestId) {
        createAndDispatch(actorUsername, receiverUsername, "FOLLOW", "Friend request", actorUsername + " sent you a friend request.", "friend_request", toLong(requestId), null, null);
    }

    private void createAndDispatch(
            String actorUsername,
            String receiverUsername,
            String type,
            String title,
            String message,
            String targetType,
            Long targetId,
            Long commentId,
            Long replyId
    ) {
        if (receiverUsername == null || receiverUsername.isBlank()) {
            return;
        }
        if (actorUsername != null && actorUsername.equals(receiverUsername)) {
            return;
        }

        User receiver = userRepository.findById(receiverUsername).orElse(null);
        if (receiver == null || receiver.getUserId() == null) {
            return;
        }

        Integer actorUserId = actorUsername == null ? null : userRepository.findUserIdByUsername(actorUsername);

        Fp20Notification notification = new Fp20Notification();
        notification.setReceiverId(String.valueOf(receiver.getUserId()));
        notification.setSenderId(actorUserId != null ? String.valueOf(actorUserId) : actorUsername);
        notification.setType(type);
        notification.setMessage(message);
        notification.setReferenceId(targetId);
        notification.setCommentId(commentId);
        notification.setReplyId(replyId);
        notification.setIsRead(false);
        Fp20Notification saved = notificationRepository.save(notification);

        Map<String, String> data = new LinkedHashMap<>();
        data.put("type", type);
        data.put("notificationId", String.valueOf(saved.getId()));
        data.put("targetType", targetType);
        data.put("targetId", targetId == null ? "" : String.valueOf(targetId));
        data.put("actorUsername", actorUsername == null ? "" : actorUsername);
        data.put("actorUserId", actorUserId == null ? "" : String.valueOf(actorUserId));
        data.put("screen", "Notification");

        pushNotificationService.sendToUser(receiver, title, message, data);
    }

    private Long toLong(Integer value) {
        return value == null ? null : value.longValue();
    }
}
