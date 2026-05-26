package com.plateapp.plate_main.notification.service;

import com.plateapp.plate_main.auth.domain.User;
import com.plateapp.plate_main.auth.repository.UserRepository;
import com.plateapp.plate_main.notification.entity.Fp21NotificationEvent;
import com.plateapp.plate_main.notification.entity.Fp22NotificationRecipient;
import com.plateapp.plate_main.notification.entity.Fp23NotificationTarget;
import com.plateapp.plate_main.notification.repository.Fp21NotificationEventRepository;
import com.plateapp.plate_main.notification.repository.Fp22NotificationRecipientRepository;
import com.plateapp.plate_main.notification.repository.Fp23NotificationTargetRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationCommandService {

    private final Fp21NotificationEventRepository eventRepository;
    private final Fp22NotificationRecipientRepository recipientRepository;
    private final Fp23NotificationTargetRepository targetRepository;
    private final UserRepository userRepository;
    private final PushNotificationService pushNotificationService;

    @Transactional
    public void notifyStoreLike(String actorUsername, String receiverUsername, Integer storeId) {
        createAndDispatch(
                actorUsername,
                receiverUsername,
                "VIDEO_LIKE",
                "새 좋아요",
                actorUsername + "님이 회원님의 동영상을 좋아합니다.",
                "store",
                toLong(storeId),
                null,
                null
        );
    }

    @Transactional
    public void notifyStoreComment(String actorUsername, String receiverUsername, Integer storeId, Integer commentId) {
        createAndDispatch(
                actorUsername,
                receiverUsername,
                "VIDEO_COMMENT",
                "새 댓글",
                actorUsername + "님이 회원님의 동영상에 댓글을 남겼습니다.",
                "store",
                toLong(storeId),
                toLong(commentId),
                null
        );
    }

    @Transactional
    public void notifyStoreReply(String actorUsername, String receiverUsername, Integer storeId, Integer commentId, Integer replyId) {
        createAndDispatch(
                actorUsername,
                receiverUsername,
                "VIDEO_REPLY",
                "새 답글",
                actorUsername + "님이 회원님의 댓글에 답글을 남겼습니다.",
                "store",
                toLong(storeId),
                toLong(commentId),
                toLong(replyId)
        );
    }

    @Transactional
    public void notifyFriendRequest(String actorUsername, String receiverUsername, Integer requestId) {
        createAndDispatch(
                actorUsername,
                receiverUsername,
                "FRIEND_REQUEST",
                "친구 요청",
                actorUsername + "님이 친구 요청을 보냈습니다.",
                "friend_request",
                toLong(requestId),
                null,
                null
        );
    }

    @Transactional
    public void notifyFeedLike(String actorUsername, String receiverUsername, Integer feedId) {
        createAndDispatch(
                actorUsername,
                receiverUsername,
                "IMAGE_FEED_LIKE",
                "새 좋아요",
                actorUsername + "님이 회원님의 이미지 피드를 좋아합니다.",
                "image_feed",
                toLong(feedId),
                null,
                null
        );
    }

    @Transactional
    public void notifyFeedComment(String actorUsername, String receiverUsername, Integer feedId, Integer commentId) {
        createAndDispatch(
                actorUsername,
                receiverUsername,
                "IMAGE_FEED_COMMENT",
                "새 댓글",
                actorUsername + "님이 회원님의 이미지 피드에 댓글을 남겼습니다.",
                "image_feed",
                toLong(feedId),
                toLong(commentId),
                null
        );
    }

    @Transactional
    public void notifyFeedReply(String actorUsername, String receiverUsername, Integer feedId, Integer commentId, Integer replyId) {
        createAndDispatch(
                actorUsername,
                receiverUsername,
                "IMAGE_FEED_REPLY",
                "새 답글",
                actorUsername + "님이 회원님의 댓글에 답글을 남겼습니다.",
                "image_feed",
                toLong(feedId),
                toLong(commentId),
                toLong(replyId)
        );
    }

    @Transactional
    public void notifyFriendAccepted(String actorUsername, String receiverUsername, Integer requestId) {
        createAndDispatch(
                actorUsername,
                receiverUsername,
                "FRIEND_ACCEPTED",
                "친구 수락",
                actorUsername + "님이 친구 요청을 수락했습니다.",
                "friend_request",
                toLong(requestId),
                null,
                null
        );
    }

    private void createAndDispatch(
            String actorUsername,
            String receiverUsername,
            String eventType,
            String title,
            String message,
            String targetType,
            Long targetId,
            Long commentId,
            Long replyId
    ) {
        if (receiverUsername == null || receiverUsername.isBlank()) {
            log.info("Skip notification dispatch: receiver username empty eventType={} actor={}", eventType, actorUsername);
            return;
        }
        if (actorUsername != null && actorUsername.equals(receiverUsername)) {
            log.info("Skip notification dispatch: actor equals receiver eventType={} username={}", eventType, actorUsername);
            return;
        }

        User receiver = userRepository.findById(receiverUsername).orElse(null);
        if (receiver == null || receiver.getUserId() == null) {
            log.info("Skip notification dispatch: receiver not found eventType={} receiver={}", eventType, receiverUsername);
            return;
        }

        Integer actorUserId = actorUsername == null ? null : userRepository.findUserIdByUsername(actorUsername);
        if (actorUserId == null) {
            log.info("Skip notification dispatch: actor not found eventType={} actor={} receiverUserId={}",
                    eventType, actorUsername, receiver.getUserId());
            return;
        }

        log.info("Create notification event eventType={} actorUserId={} receiverUserId={} targetType={} targetId={} commentId={} replyId={}",
                eventType, actorUserId, receiver.getUserId(), targetType, targetId, commentId, replyId);

        Fp21NotificationEvent event = new Fp21NotificationEvent();
        event.setEventType(eventType);
        event.setActorUserId(actorUserId);
        event.setObjectType(resolveObjectType(eventType));
        event.setObjectId(targetId);
        event.setParentObjectType(resolveParentObjectType(eventType));
        event.setParentObjectId(resolveParentObjectId(eventType, commentId, replyId));
        event.setMessageTemplate(message);
        event.setMessageParams(buildMessageParams(actorUsername, targetType, targetId, commentId, replyId));
        event = eventRepository.save(event);

        Fp22NotificationRecipient recipient = new Fp22NotificationRecipient();
        recipient.setEventId(event.getEventId());
        recipient.setRecipientUserId(receiver.getUserId());
        recipient.setInboxStatus("ACTIVE");
        recipient.setIsRead(false);
        recipient.setIsDeleted(false);
        recipient = recipientRepository.save(recipient);

        Fp23NotificationTarget target = new Fp23NotificationTarget();
        target.setEventId(event.getEventId());
        target.setTargetType(targetType);
        target.setTargetId(targetId);
        target.setTargetSubId(resolveTargetSubId(eventType, commentId, replyId));
        target.setDeepLink(buildDeepLink(targetType, targetId, commentId, replyId));
        target.setWebPath(buildWebPath(targetType, targetId));
        target.setAppRoute(buildAppRoute(targetType));
        targetRepository.save(target);

        Map<String, String> data = new LinkedHashMap<>();
        data.put("type", eventType);
        data.put("notificationId", String.valueOf(recipient.getNotificationId()));
        data.put("targetType", targetType);
        data.put("targetId", targetId == null ? "" : String.valueOf(targetId));
        data.put("actorUsername", actorUsername == null ? "" : actorUsername);
        data.put("actorUserId", String.valueOf(actorUserId));
        data.put("screen", "Notification");

        boolean pushSent = pushNotificationService.sendToUser(receiver, recipient.getNotificationId(), title, message, data);
        log.info("Notification dispatch completed eventId={} notificationId={} eventType={} receiverUserId={} pushSent={}",
                event.getEventId(), recipient.getNotificationId(), eventType, receiver.getUserId(), pushSent);
    }

    private Map<String, Object> buildMessageParams(
            String actorUsername,
            String targetType,
            Long targetId,
            Long commentId,
            Long replyId
    ) {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("actorUsername", actorUsername);
        params.put("targetType", targetType);
        params.put("targetId", targetId);
        params.put("commentId", commentId);
        params.put("replyId", replyId);
        return params;
    }

    private String resolveObjectType(String eventType) {
        return switch (eventType) {
            case "VIDEO_COMMENT" -> "comment";
            case "VIDEO_REPLY" -> "reply";
            case "IMAGE_FEED_COMMENT" -> "comment";
            case "IMAGE_FEED_REPLY" -> "reply";
            case "FRIEND_REQUEST", "FRIEND_ACCEPTED" -> "friend_request";
            default -> "content";
        };
    }

    private String resolveParentObjectType(String eventType) {
        return switch (eventType) {
            case "VIDEO_COMMENT" -> "comment";
            case "VIDEO_REPLY" -> "comment";
            case "IMAGE_FEED_COMMENT" -> "comment";
            case "IMAGE_FEED_REPLY" -> "comment";
            default -> null;
        };
    }

    private Long resolveParentObjectId(String eventType, Long commentId, Long replyId) {
        return switch (eventType) {
            case "VIDEO_COMMENT" -> commentId;
            case "VIDEO_REPLY" -> commentId;
            case "IMAGE_FEED_COMMENT" -> commentId;
            case "IMAGE_FEED_REPLY" -> commentId;
            default -> null;
        };
    }

    private Long resolveTargetSubId(String eventType, Long commentId, Long replyId) {
        return switch (eventType) {
            case "VIDEO_COMMENT" -> commentId;
            case "VIDEO_REPLY" -> replyId;
            case "IMAGE_FEED_COMMENT" -> commentId;
            case "IMAGE_FEED_REPLY" -> replyId;
            default -> null;
        };
    }

    private String buildDeepLink(String targetType, Long targetId, Long commentId, Long replyId) {
        if (targetId == null) {
            return null;
        }
        return switch (targetType) {
            case "store" -> {
                if (replyId != null) {
                    yield "plate://videos/" + targetId + "?replyId=" + replyId;
                }
                if (commentId != null) {
                    yield "plate://videos/" + targetId + "?commentId=" + commentId;
                }
                yield "plate://videos/" + targetId;
            }
            case "image_feed" -> "plate://image-feeds/" + targetId;
            case "friend_request" -> "plate://friends/requests";
            default -> null;
        };
    }

    private String buildWebPath(String targetType, Long targetId) {
        if (targetId == null) {
            return null;
        }
        return switch (targetType) {
            case "store" -> "/videos/" + targetId;
            case "image_feed" -> "/image-feeds/" + targetId;
            case "friend_request" -> "/friends/requests";
            default -> null;
        };
    }

    private String buildAppRoute(String targetType) {
        return switch (targetType) {
            case "store" -> "VideoDetail";
            case "image_feed" -> "ImageFeedDetail";
            case "friend_request" -> "FriendRequests";
            default -> null;
        };
    }

    private Long toLong(Integer value) {
        return value == null ? null : value.longValue();
    }
}
