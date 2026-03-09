package com.plateapp.plate_main.notification.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.plateapp.plate_main.notification.dto.NotificationDtos;
import com.plateapp.plate_main.notification.service.NotificationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

  private final NotificationService notificationService;

  private String currentUsername() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || auth.getName() == null || auth.getName().isBlank()) {
      throw new IllegalStateException("Unauthenticated");
    }
    return auth.getName();
  }

  @GetMapping
  public ResponseEntity<?> listNotifications(
      @RequestParam(name = "limit", defaultValue = "20") int limit,
      @RequestParam(name = "offset", defaultValue = "0") int offset,
      @RequestParam(name = "unreadOnly", defaultValue = "false") boolean unreadOnly
  ) {
    String username = currentUsername();
    List<NotificationDtos.NotificationResponse> items =
        notificationService.listNotifications(username, limit, offset, unreadOnly);
    return ResponseEntity.ok(Map.of("items", items));
  }

  @GetMapping("/unread-count")
  public ResponseEntity<?> unreadCount() {
    String username = currentUsername();
    long count = notificationService.unreadCount(username);
    return ResponseEntity.ok(Map.of("count", count));
  }

  @PutMapping("/{notificationId}/read")
  public ResponseEntity<?> markRead(@PathVariable("notificationId") Long notificationId) {
    String username = currentUsername();
    notificationService.markRead(username, notificationId);
    return ResponseEntity.noContent().build();
  }

  @PutMapping("/read-all")
  public ResponseEntity<?> markAllRead() {
    String username = currentUsername();
    notificationService.markAllRead(username);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{notificationId}")
  public ResponseEntity<?> deleteOne(@PathVariable("notificationId") Long notificationId) {
    String username = currentUsername();
    notificationService.deleteOne(username, notificationId);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/all")
  public ResponseEntity<?> deleteAll() {
    String username = currentUsername();
    notificationService.deleteAll(username);
    return ResponseEntity.noContent().build();
  }
}
