package com.plateapp.plate_main.recommendation.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "fp_370")
public class Fp370VideoEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Long eventId;

    @Column(name = "event_uid", length = 100)
    private String eventUid;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "username", length = 50)
    private String username;

    @Column(name = "is_guest", nullable = false)
    private Boolean isGuest = false;

    @Column(name = "guest_id", length = 100)
    private String guestId;

    @Column(name = "session_id", length = 150)
    private String sessionId;

    @Column(name = "device_id", length = 200)
    private String deviceId;

    @Column(name = "store_id", nullable = false)
    private Integer storeId;

    @Column(name = "place_id")
    private String placeId;

    @Column(name = "creator_username", length = 50)
    private String creatorUsername;

    @Column(name = "event_type", nullable = false, length = 40)
    private String eventType;

    @Column(name = "event_source", nullable = false, length = 40)
    private String eventSource = "HOME";

    @Column(name = "request_id", length = 100)
    private String requestId;

    @Column(name = "algorithm_version", length = 50)
    private String algorithmVersion;

    @Column(name = "impression_position")
    private Integer impressionPosition;

    @Column(name = "play_position_ms")
    private Integer playPositionMs;

    @Column(name = "watch_duration_ms")
    private Integer watchDurationMs;

    @Column(name = "video_duration_ms")
    private Integer videoDurationMs;

    @Column(name = "completion_ratio", precision = 6, scale = 5)
    private BigDecimal completionRatio;

    @Column(name = "client_event_at")
    private LocalDateTime clientEventAt;

    @CreationTimestamp
    @Column(name = "server_event_at", nullable = false, updatable = false)
    private LocalDateTime serverEventAt;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    private String metadata;
}
