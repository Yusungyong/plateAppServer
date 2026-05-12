package com.plateapp.plate_main.notification.service;

import com.plateapp.plate_main.auth.repository.UserRepository;
import com.plateapp.plate_main.notification.dto.NotificationDtos;
import com.plateapp.plate_main.notification.entity.Fp21NotificationEvent;
import com.plateapp.plate_main.notification.entity.Fp22NotificationRecipient;
import com.plateapp.plate_main.notification.entity.Fp23NotificationTarget;
import com.plateapp.plate_main.notification.repository.Fp21NotificationEventRepository;
import com.plateapp.plate_main.notification.repository.Fp22NotificationRecipientRepository;
import com.plateapp.plate_main.notification.repository.Fp23NotificationTargetRepository;
import com.plateapp.plate_main.user.entity.Fp100User;
import com.plateapp.plate_main.user.repository.MemberRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final Fp22NotificationRecipientRepository recipientRepository;
    private final Fp21NotificationEventRepository eventRepository;
    private final Fp23NotificationTargetRepository targetRepository;
    private final UserRepository userRepository;
    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public List<NotificationDtos.NotificationResponse> listNotifications(
            String username, int limit, int offset, boolean unreadOnly
    ) {
        Integer userId = userRepository.findUserIdByUsername(username);
        if (userId == null) {
            throw new IllegalArgumentException("User not found");
        }

        int safeLimit = Math.min(Math.max(limit, 1), 100);
        int safeOffset = Math.max(offset, 0);
        Pageable pageable = PageRequest.of(safeOffset / safeLimit, safeLimit,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Fp22NotificationRecipient> page = unreadOnly
                ? recipientRepository.findUnreadActiveByRecipientUserId(userId, pageable)
                : recipientRepository.findActiveByRecipientUserId(userId, pageable);

        List<Fp22NotificationRecipient> recipients = page.getContent();
        Map<Long, Fp21NotificationEvent> events = eventRepository.findByEventIdIn(extractEventIds(recipients)).stream()
                .collect(Collectors.toMap(Fp21NotificationEvent::getEventId, Function.identity()));
        Map<Long, Fp23NotificationTarget> targets = targetRepository.findByEventIdIn(events.keySet()).stream()
                .collect(Collectors.toMap(Fp23NotificationTarget::getEventId, Function.identity()));

        List<Integer> actorUserIds = events.values().stream()
                .map(Fp21NotificationEvent::getActorUserId)
                .distinct()
                .toList();
        Map<Integer, Fp100User> actorMap = new HashMap<>();
        if (!actorUserIds.isEmpty()) {
            for (Fp100User actor : memberRepository.findByUserIdIn(actorUserIds)) {
                if (actor.getUserId() != null) {
                    actorMap.put(actor.getUserId(), actor);
                }
            }
        }

        List<NotificationDtos.NotificationResponse> items = new ArrayList<>();
        for (Fp22NotificationRecipient recipient : recipients) {
            Fp21NotificationEvent event = events.get(recipient.getEventId());
            if (event == null) {
                continue;
            }
            Fp23NotificationTarget target = targets.get(recipient.getEventId());
            Fp100User actor = actorMap.get(event.getActorUserId());

            NotificationDtos.NotificationResponse response = new NotificationDtos.NotificationResponse();
            response.notificationId = recipient.getNotificationId();
            response.userId = userId;
            response.type = normalizeType(event.getEventType());
            response.title = defaultTitle(response.type);
            response.message = event.getMessageTemplate();
            response.targetType = target == null ? null : target.getTargetType();
            response.targetId = target == null ? null : target.getTargetId();
            response.commentId = resolveCommentId(event, target);
            response.replyId = resolveReplyId(event, target);
            response.isRead = Boolean.TRUE.equals(recipient.getIsRead());
            response.readAt = recipient.getReadAt();
            response.createdAt = recipient.getCreatedAt();
            response.actorUserId = event.getActorUserId();
            response.actorUsername = actor == null ? null : actor.getUsername();
            response.actorProfileImageUrl = actor == null ? null : actor.getProfileImageUrl();
            response.data = buildData(target);
            items.add(response);
        }

        return items;
    }

    @Transactional(readOnly = true)
    public long unreadCount(String username) {
        Integer userId = userRepository.findUserIdByUsername(username);
        if (userId == null) {
            throw new IllegalArgumentException("User not found");
        }
        return recipientRepository.countUnreadActiveByRecipientUserId(userId);
    }

    @Transactional
    public void markRead(String username, Long notificationId) {
        Integer userId = userRepository.findUserIdByUsername(username);
        if (userId == null) {
            throw new IllegalArgumentException("User not found");
        }

        int updated = recipientRepository.markRead(notificationId, userId);
        if (updated == 0) {
            throw new NoSuchElementException("notification not found");
        }
    }

    @Transactional
    public void markAllRead(String username) {
        Integer userId = userRepository.findUserIdByUsername(username);
        if (userId == null) {
            throw new IllegalArgumentException("User not found");
        }
        recipientRepository.markAllRead(userId);
    }

    @Transactional
    public void deleteOne(String username, Long notificationId) {
        Integer userId = userRepository.findUserIdByUsername(username);
        if (userId == null) {
            throw new IllegalArgumentException("User not found");
        }

        int updated = recipientRepository.softDeleteOne(notificationId, userId);
        if (updated == 0) {
            throw new NoSuchElementException("notification not found");
        }
    }

    @Transactional
    public void deleteAll(String username) {
        Integer userId = userRepository.findUserIdByUsername(username);
        if (userId == null) {
            throw new IllegalArgumentException("User not found");
        }
        recipientRepository.softDeleteAll(userId);
    }

    private Collection<Long> extractEventIds(List<Fp22NotificationRecipient> recipients) {
        return recipients.stream()
                .map(Fp22NotificationRecipient::getEventId)
                .distinct()
                .toList();
    }

    private Long resolveCommentId(Fp21NotificationEvent event, Fp23NotificationTarget target) {
        if ("VIDEO_COMMENT".equals(event.getEventType())) {
            return target == null ? null : target.getTargetSubId();
        }
        if ("VIDEO_REPLY".equals(event.getEventType())) {
            return event.getParentObjectId();
        }
        return null;
    }

    private Long resolveReplyId(Fp21NotificationEvent event, Fp23NotificationTarget target) {
        if ("VIDEO_REPLY".equals(event.getEventType())) {
            return target == null ? null : target.getTargetSubId();
        }
        return null;
    }

    private NotificationDtos.Data buildData(Fp23NotificationTarget target) {
        if (target == null || target.getDeepLink() == null || target.getDeepLink().isBlank()) {
            return null;
        }
        NotificationDtos.Data data = new NotificationDtos.Data();
        data.deepLink = target.getDeepLink();
        return data;
    }

    private String normalizeType(String type) {
        if (type == null) {
            return null;
        }
        return type.trim().toUpperCase(Locale.ROOT);
    }

    private String defaultTitle(String type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case "VIDEO_LIKE", "IMAGE_FEED_LIKE" -> "Like";
            case "VIDEO_COMMENT" -> "Comment";
            case "VIDEO_REPLY" -> "Reply";
            case "FRIEND_REQUEST" -> "Friend request";
            default -> type;
        };
    }
}
