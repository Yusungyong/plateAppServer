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
@Table(name = "fp_373")
public class Fp373HomeRecommendationCandidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "candidate_id")
    private Long candidateId;

    @Column(name = "batch_key", nullable = false, length = 100)
    private String batchKey;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "username", length = 50)
    private String username;

    @Column(name = "is_guest", nullable = false)
    private Boolean isGuest = false;

    @Column(name = "guest_id", length = 100)
    private String guestId;

    @Column(name = "store_id", nullable = false)
    private Integer storeId;

    @Column(name = "candidate_source", nullable = false, length = 40)
    private String candidateSource;

    @Column(name = "base_score", nullable = false, precision = 12, scale = 6)
    private BigDecimal baseScore = BigDecimal.ZERO;

    @Column(name = "rank_score", nullable = false, precision = 12, scale = 6)
    private BigDecimal rankScore = BigDecimal.ZERO;

    @Column(name = "reason_code", length = 80)
    private String reasonCode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "reason_payload")
    private String reasonPayload;

    @Column(name = "algorithm_version", nullable = false, length = 50)
    private String algorithmVersion = "v1";

    @CreationTimestamp
    @Column(name = "generated_at", nullable = false, updatable = false)
    private LocalDateTime generatedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}
