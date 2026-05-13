package com.plateapp.plate_main.recommendation.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "fp_372")
public class Fp372VideoFeature {

    @Id
    @Column(name = "store_id")
    private Integer storeId;

    @Column(name = "place_id")
    private String placeId;

    @Column(name = "creator_username", length = 50)
    private String creatorUsername;

    @Column(name = "store_name")
    private String storeName;

    @Column(name = "title")
    private String title;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "region_1", length = 100)
    private String region1;

    @Column(name = "region_2", length = 100)
    private String region2;

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "video_duration")
    private Integer videoDuration;

    @Column(name = "duration_bucket", length = 40)
    private String durationBucket;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags")
    private String tags;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "categories")
    private String categories;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "feature_vector")
    private String featureVector;

    @Column(name = "like_count", nullable = false)
    private Long likeCount = 0L;

    @Column(name = "comment_count", nullable = false)
    private Long commentCount = 0L;

    @Column(name = "impression_count", nullable = false)
    private Long impressionCount = 0L;

    @Column(name = "click_count", nullable = false)
    private Long clickCount = 0L;

    @Column(name = "play_count", nullable = false)
    private Long playCount = 0L;

    @Column(name = "complete_count", nullable = false)
    private Long completeCount = 0L;

    @Column(name = "hide_count", nullable = false)
    private Long hideCount = 0L;

    @Column(name = "report_count", nullable = false)
    private Long reportCount = 0L;

    @Column(name = "popularity_score", nullable = false, precision = 12, scale = 6)
    private BigDecimal popularityScore = BigDecimal.ZERO;

    @Column(name = "quality_score", nullable = false, precision = 12, scale = 6)
    private BigDecimal qualityScore = BigDecimal.ZERO;

    @Column(name = "freshness_score", nullable = false, precision = 12, scale = 6)
    private BigDecimal freshnessScore = BigDecimal.ZERO;

    @Column(name = "content_created_at")
    private LocalDate contentCreatedAt;

    @Column(name = "content_updated_at")
    private LocalDate contentUpdatedAt;

    @Column(name = "feature_refreshed_at", nullable = false)
    private LocalDateTime featureRefreshedAt = LocalDateTime.now();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
