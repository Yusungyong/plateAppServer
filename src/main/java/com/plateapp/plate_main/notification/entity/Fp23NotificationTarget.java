package com.plateapp.plate_main.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "fp_23")
public class Fp23NotificationTarget {

    @Id
    @Column(name = "event_id")
    private Long eventId;

    @Column(name = "target_type", nullable = false, length = 50)
    private String targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "target_sub_id")
    private Long targetSubId;

    @Column(name = "deep_link", length = 500)
    private String deepLink;

    @Column(name = "web_path", length = 500)
    private String webPath;

    @Column(name = "app_route", length = 200)
    private String appRoute;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
    public Long getTargetSubId() { return targetSubId; }
    public void setTargetSubId(Long targetSubId) { this.targetSubId = targetSubId; }
    public String getDeepLink() { return deepLink; }
    public void setDeepLink(String deepLink) { this.deepLink = deepLink; }
    public String getWebPath() { return webPath; }
    public void setWebPath(String webPath) { this.webPath = webPath; }
    public String getAppRoute() { return appRoute; }
    public void setAppRoute(String appRoute) { this.appRoute = appRoute; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
