package com.plateapp.plate_main.admin.contentverification.entity;

import jakarta.persistence.*;
import java.time.*;
import lombok.Getter;

@Entity @Table(name="content_verification_history") @Getter
public class ContentVerificationHistory {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="verification_id", nullable=false) private Long verificationId;
    @Column(nullable=false, length=40) private String action;
    @Column(name="previous_status", length=40) private String previousStatus;
    @Column(name="next_status", length=40) private String nextStatus;
    @Column(name="actor_user_id", nullable=false) private Integer actorUserId;
    @Column(name="assignee_user_id") private Integer assigneeUserId;
    @Column(columnDefinition="text") private String reason;
    @Column(name="created_at", nullable=false) private OffsetDateTime createdAt;
    protected ContentVerificationHistory() {}
    public static ContentVerificationHistory create(Long verificationId, String action, String previousStatus,
            String nextStatus, Integer actorUserId, Integer assigneeUserId, String reason) {
        ContentVerificationHistory h = new ContentVerificationHistory();
        h.verificationId=verificationId; h.action=action; h.previousStatus=previousStatus; h.nextStatus=nextStatus;
        h.actorUserId=actorUserId; h.assigneeUserId=assigneeUserId; h.reason=reason;
        h.createdAt=OffsetDateTime.now(ZoneOffset.UTC); return h;
    }
}
