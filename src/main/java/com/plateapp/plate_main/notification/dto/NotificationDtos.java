package com.plateapp.plate_main.notification.dto;

import java.time.LocalDateTime;

public class NotificationDtos {

  public static class Data {
    public String deepLink;
  }

  public static class NotificationResponse {
    public Long notificationId;
    public Integer userId;
    public String type;
    public String title;
    public String message;
    public String targetType;
    public Long targetId;
    public Boolean isRead;
    public LocalDateTime readAt;
    public LocalDateTime createdAt;
    public Integer actorUserId;
    public String actorUsername;
    public String actorProfileImageUrl;
    public Data data;
  }
}
