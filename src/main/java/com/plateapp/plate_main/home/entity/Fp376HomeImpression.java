package com.plateapp.plate_main.home.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

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
@Table(name = "fp_376")
public class Fp376HomeImpression {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "impression_id")
    private Long impressionId;

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

    @Column(name = "content_type", nullable = false, length = 20)
    private String contentType;

    @Column(name = "store_id")
    private Integer storeId;

    @Column(name = "feed_no")
    private Integer feedNo;

    @Column(name = "surface", nullable = false, length = 60)
    private String surface = "home";

    @Column(name = "request_id", length = 100)
    private String requestId;

    @Column(name = "position_no")
    private Integer positionNo;

    @Column(name = "client_impressed_at")
    private LocalDateTime clientImpressedAt;

    @CreationTimestamp
    @Column(name = "impressed_at", nullable = false, updatable = false)
    private LocalDateTime impressedAt;
}
