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
@Table(name = "store_application_change_requests")
@Getter
public class StoreApplicationChangeRequest {

    public static final String STATUS_OPEN = "open";
    public static final String STATUS_RESUBMITTED = "resubmitted";
    public static final String STATUS_CLOSED = "closed";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Column(name = "review_id")
    private Long reviewId;

    @Column(name = "applicant_message", nullable = false, columnDefinition = "text")
    private String applicantMessage;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "requested_by", nullable = false)
    private Integer requestedBy;

    @Column(name = "requested_at", nullable = false)
    private OffsetDateTime requestedAt;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @Column(name = "request_id", length = 100)
    private String requestId;

    protected StoreApplicationChangeRequest() {
    }

    public static StoreApplicationChangeRequest create(
            Long applicationId,
            Long reviewId,
            String applicantMessage,
            Integer requestedBy,
            OffsetDateTime requestedAt,
            String requestId
    ) {
        StoreApplicationChangeRequest request = new StoreApplicationChangeRequest();
        request.applicationId = applicationId;
        request.reviewId = reviewId;
        request.applicantMessage = applicantMessage;
        request.status = STATUS_OPEN;
        request.requestedBy = requestedBy;
        request.requestedAt = requestedAt;
        request.requestId = requestId;
        return request;
    }

    public void markResubmitted(OffsetDateTime now) {
        this.status = STATUS_RESUBMITTED;
        this.resolvedAt = now;
    }
}
