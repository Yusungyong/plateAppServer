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
@Table(name = "fp_375")
public class Fp375RecommendationServingItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "serving_item_id")
    private Long servingItemId;

    @Column(name = "serving_id", nullable = false)
    private Long servingId;

    @Column(name = "request_id", nullable = false, length = 100)
    private String requestId;

    @Column(name = "store_id", nullable = false)
    private Integer storeId;

    @Column(name = "position_no", nullable = false)
    private Integer positionNo;

    @Column(name = "candidate_source", length = 40)
    private String candidateSource;

    @Column(name = "rank_score", precision = 12, scale = 6)
    private BigDecimal rankScore;

    @Column(name = "reason_code", length = 80)
    private String reasonCode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "reason_payload")
    private String reasonPayload;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
