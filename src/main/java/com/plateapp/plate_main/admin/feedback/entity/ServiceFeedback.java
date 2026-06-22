package com.plateapp.plate_main.admin.feedback.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.Getter;

@Entity
@Table(name = "service_feedback")
@Getter
public class ServiceFeedback {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 40)
    private String type;
    @Column(nullable = false, columnDefinition = "text")
    private String content;
    @Column(nullable = false, length = 40)
    private String status;
    @Column(length = 320)
    private String contact;
    @Column(name = "contact_purge_at")
    private OffsetDateTime contactPurgeAt;
    @Column(name = "requester_user_id")
    private Integer requesterUserId;
    @Column(name = "assignee_user_id")
    private Integer assigneeUserId;
    @Column(name = "internal_memo", columnDefinition = "text")
    private String internalMemo;
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
    @Version
    private Long version;

    protected ServiceFeedback() {}

    public static ServiceFeedback create(String type, String content, String contact, Integer requesterUserId,
            int contactRetentionDays) {
        ServiceFeedback feedback = new ServiceFeedback();
        feedback.type = type;
        feedback.content = content;
        feedback.contact = contact;
        feedback.requesterUserId = requesterUserId;
        feedback.status = "received";
        feedback.createdAt = feedback.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
        feedback.contactPurgeAt = contact == null ? null : feedback.createdAt.plusDays(contactRetentionDays);
        return feedback;
    }

    public void update(String status, Integer assigneeUserId, String internalMemo) {
        if (status != null) this.status = status;
        this.assigneeUserId = assigneeUserId;
        this.internalMemo = internalMemo;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
