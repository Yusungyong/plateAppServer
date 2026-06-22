package com.plateapp.plate_main.admin.contentverification.entity;

import jakarta.persistence.*;
import java.time.*;
import lombok.Getter;

@Entity @Table(name = "content_verifications") @Getter
public class ContentVerification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name="target_type", nullable=false, length=40) private String targetType;
    @Column(name="target_id", nullable=false, length=100) private String targetId;
    @Column(nullable=false, length=40) private String status;
    @Column(name="requester_user_id") private Integer requesterUserId;
    @Column(name="assignee_user_id") private Integer assigneeUserId;
    @Column(name="review_reason", columnDefinition="text") private String reviewReason;
    @Column(name="created_at", nullable=false) private OffsetDateTime createdAt;
    @Column(name="updated_at", nullable=false) private OffsetDateTime updatedAt;
    @Version private Long version;
    protected ContentVerification() {}
    public static ContentVerification create(String targetType, String targetId, Integer requesterUserId) {
        ContentVerification verification = new ContentVerification();
        verification.targetType = targetType;
        verification.targetId = targetId;
        verification.requesterUserId = requesterUserId;
        verification.status = "pending";
        verification.createdAt = verification.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
        return verification;
    }
    public void assign(Integer assigneeUserId) {
        this.assigneeUserId = assigneeUserId;
        if ("pending".equals(status)) status = "in_review";
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
    public void decide(String nextStatus, String reason) {
        status = nextStatus; reviewReason = reason; updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
