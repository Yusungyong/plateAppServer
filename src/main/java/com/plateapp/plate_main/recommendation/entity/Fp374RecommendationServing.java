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
@Table(name = "fp_374")
public class Fp374RecommendationServing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "serving_id")
    private Long servingId;

    @Column(name = "request_id", nullable = false, length = 100)
    private String requestId;

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

    @Column(name = "endpoint", nullable = false, length = 100)
    private String endpoint = "/api/home/video-thumbnails";

    @Column(name = "page_no")
    private Integer pageNo;

    @Column(name = "page_size")
    private Integer pageSize;

    @Column(name = "sort_type", length = 40)
    private String sortType;

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "radius_meters", precision = 10, scale = 2)
    private BigDecimal radiusMeters;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "place_ids")
    private String placeIds;

    @Column(name = "algorithm_version", nullable = false, length = 50)
    private String algorithmVersion = "v1";

    @Column(name = "candidate_count", nullable = false)
    private Integer candidateCount = 0;

    @Column(name = "served_count", nullable = false)
    private Integer servedCount = 0;

    @Column(name = "fallback_used", nullable = false)
    private Boolean fallbackUsed = false;

    @CreationTimestamp
    @Column(name = "served_at", nullable = false, updatable = false)
    private LocalDateTime servedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    private String metadata;
}
