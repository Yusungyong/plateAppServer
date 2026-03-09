package com.plateapp.plate_main.notification.service;

import java.util.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plateapp.plate_main.auth.repository.UserRepository;
import com.plateapp.plate_main.notification.dto.NotificationDtos;
import com.plateapp.plate_main.notification.entity.Fp20Notification;
import com.plateapp.plate_main.notification.repository.NotificationRepository;
import com.plateapp.plate_main.user.entity.Fp100User;
import com.plateapp.plate_main.user.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {

  private final NotificationRepository notificationRepository;
  private final UserRepository userRepository;
  private final MemberRepository memberRepository;

  @Transactional(readOnly = true)
  public List<NotificationDtos.NotificationResponse> listNotifications(
      String username, int limit, int offset, boolean unreadOnly
  ) {
    Integer userId = userRepository.findUserIdByUsername(username);
    if (userId == null) throw new IllegalArgumentException("User not found");

    int safeLimit = Math.min(Math.max(limit, 1), 100);
    int safeOffset = Math.max(offset, 0);
    Pageable pageable = PageRequest.of(safeOffset / safeLimit, safeLimit,
        Sort.by(Sort.Direction.DESC, "createdAt"));

    String receiverId = String.valueOf(userId);
    Page<Fp20Notification> page = unreadOnly
        ? notificationRepository.findUnreadByReceiverId(receiverId, pageable)
        : notificationRepository.findByReceiverId(receiverId, pageable);

    List<Fp20Notification> notifications = page.getContent();
    Map<Integer, Fp100User> userIdMap = new HashMap<>();
    Map<String, Fp100User> usernameMap = new HashMap<>();

    List<Integer> senderUserIds = new ArrayList<>();
    List<String> senderUsernames = new ArrayList<>();
    for (Fp20Notification n : notifications) {
      String senderId = n.getSenderId();
      Integer numericId = parseInt(senderId);
      if (numericId != null) {
        senderUserIds.add(numericId);
      } else if (senderId != null && !senderId.isBlank()) {
        senderUsernames.add(senderId);
      }
    }

    if (!senderUserIds.isEmpty()) {
      for (Fp100User u : memberRepository.findByUserIdIn(senderUserIds)) {
        if (u.getUserId() != null) userIdMap.put(u.getUserId(), u);
      }
    }
    if (!senderUsernames.isEmpty()) {
      for (Fp100User u : memberRepository.findByUsernameIn(senderUsernames)) {
        if (u.getUsername() != null) usernameMap.put(u.getUsername(), u);
      }
    }

    List<NotificationDtos.NotificationResponse> items = new ArrayList<>();
    for (Fp20Notification n : notifications) {
      NotificationDtos.NotificationResponse r = new NotificationDtos.NotificationResponse();
      r.notificationId = n.getId();
      r.userId = userId;
      r.type = normalizeType(n.getType());
      r.title = defaultTitle(r.type);
      r.message = n.getMessage();
      r.targetType = null;
      r.targetId = n.getReferenceId();
      r.isRead = n.getIsRead() != null ? n.getIsRead() : false;
      r.readAt = null;
      r.createdAt = n.getCreatedAt();

      String senderId = n.getSenderId();
      Integer senderUserId = parseInt(senderId);
      if (senderUserId != null) {
        Fp100User actor = userIdMap.get(senderUserId);
        r.actorUserId = senderUserId;
        r.actorUsername = actor != null ? actor.getUsername() : null;
        r.actorProfileImageUrl = actor != null ? actor.getProfileImageUrl() : null;
      } else if (senderId != null && !senderId.isBlank()) {
        Fp100User actor = usernameMap.get(senderId);
        r.actorUserId = actor != null ? actor.getUserId() : null;
        r.actorUsername = actor != null ? actor.getUsername() : senderId;
        r.actorProfileImageUrl = actor != null ? actor.getProfileImageUrl() : null;
      }

      r.data = null;
      items.add(r);
    }

    return items;
  }

  @Transactional(readOnly = true)
  public long unreadCount(String username) {
    Integer userId = userRepository.findUserIdByUsername(username);
    if (userId == null) throw new IllegalArgumentException("User not found");
    return notificationRepository.countUnreadByReceiverId(String.valueOf(userId));
  }

  @Transactional
  public void markRead(String username, Long notificationId) {
    Integer userId = userRepository.findUserIdByUsername(username);
    if (userId == null) throw new IllegalArgumentException("User not found");

    int updated = notificationRepository.markRead(notificationId, String.valueOf(userId));
    if (updated == 0) throw new NoSuchElementException("notification not found");
  }

  @Transactional
  public void markAllRead(String username) {
    Integer userId = userRepository.findUserIdByUsername(username);
    if (userId == null) throw new IllegalArgumentException("User not found");

    notificationRepository.markAllRead(String.valueOf(userId));
  }

  @Transactional
  public void deleteOne(String username, Long notificationId) {
    Integer userId = userRepository.findUserIdByUsername(username);
    if (userId == null) throw new IllegalArgumentException("User not found");

    int deleted = notificationRepository.deleteByIdAndReceiverId(notificationId, String.valueOf(userId));
    if (deleted == 0) throw new NoSuchElementException("notification not found");
  }

  @Transactional
  public void deleteAll(String username) {
    Integer userId = userRepository.findUserIdByUsername(username);
    if (userId == null) throw new IllegalArgumentException("User not found");

    notificationRepository.deleteByReceiverId(String.valueOf(userId));
  }

  private static Integer parseInt(String value) {
    if (value == null) return null;
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static String normalizeType(String type) {
    if (type == null) return null;
    return type.trim().toUpperCase(Locale.ROOT);
  }

  private static String defaultTitle(String type) {
    if (type == null) return null;
    return switch (type) {
      case "LIKE" -> "Like";
      case "COMMENT" -> "Comment";
      case "REPLY" -> "Reply";
      case "FOLLOW" -> "Follow";
      case "MENTION" -> "Mention";
      case "SYSTEM" -> "System";
      default -> type;
    };
  }
}
