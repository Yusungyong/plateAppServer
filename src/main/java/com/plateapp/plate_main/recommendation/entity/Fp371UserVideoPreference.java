package com.plateapp.plate_main.recommendation.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
@Table(name = "fp_371")
public class Fp371UserVideoPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "preference_id")
    private Long preferenceId;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "username", length = 50)
    private String username;

    @Column(name = "is_guest", nullable = false)
    private Boolean isGuest = false;

    @Column(name = "guest_id", length = 100)
    private String guestId;

    @Column(name = "subject_type", nullable = false, length = 40)
    private String subjectType;

    @Column(name = "subject_key", nullable = false)
    private String subjectKey;

    @Column(name = "score", nullable = false, precision = 12, scale = 6)
    private BigDecimal score = BigDecimal.ZERO;

    @Column(name = "positive_count", nullable = false)
    private Integer positiveCount = 0;

    @Column(name = "negative_count", nullable = false)
    private Integer negativeCount = 0;

    @Column(name = "impression_count", nullable = false)
    private Integer impressionCount = 0;

    @Column(name = "last_event_at")
    private LocalDateTime lastEventAt;

    @Column(name = "model_version", nullable = false, length = 50)
    private String modelVersion = "v1";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
