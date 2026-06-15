package com.plateapp.plate_main.admin.storeapproval.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;

@Entity
@Table(name = "store_application_reviews")
@Getter
public class StoreApplicationReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Column(name = "previous_status", nullable = false, length = 30)
    private String previousStatus;

    @Column(name = "next_status", nullable = false, length = 30)
    private String nextStatus;

    @Column(name = "reason_code", length = 80)
    private String reasonCode;

    @Column(name = "reason", columnDefinition = "text")
    private String reason;

    @Column(name = "comment", columnDefinition = "text")
    private String comment;

    @Column(name = "reviewed_by", nullable = false)
    private Integer reviewedBy;

    @Column(name = "reviewed_at", nullable = false)
    private OffsetDateTime reviewedAt;

    @Column(name = "request_id", length = 100)
    private String requestId;

    protected StoreApplicationReview() {
    }

    public static StoreApplicationReview create(
            Long applicationId,
            String previousStatus,
            String nextStatus,
            String reasonCode,
            String reason,
            String comment,
            Integer reviewedBy,
            OffsetDateTime reviewedAt,
            String requestId
    ) {
        StoreApplicationReview review = new StoreApplicationReview();
        review.applicationId = applicationId;
        review.previousStatus = previousStatus;
        review.nextStatus = nextStatus;
        review.reasonCode = reasonCode;
        review.reason = reason;
        review.comment = comment;
        review.reviewedBy = reviewedBy;
        review.reviewedAt = reviewedAt;
        review.requestId = requestId;
        return review;
    }
}
