package com.plateapp.plate_main.admin.feedmoderation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.Getter;

@Entity
@Table(name = "admin_feed_moderation")
@Getter
public class AdminFeedModeration {
    @Id
    @Column(name = "feed_id")
    private Integer feedId;

    @Column(name = "visibility_status", nullable = false, length = 30)
    private String visibilityStatus;

    @Column(name = "recommended", nullable = false)
    private Boolean recommended;

    @Column(name = "reason", columnDefinition = "text")
    private String reason;

    @Column(name = "moderated_by")
    private Integer moderatedBy;

    @Column(name = "moderated_at")
    private OffsetDateTime moderatedAt;

    @Version
    private Long version = 0L;

    protected AdminFeedModeration() {}

    public static AdminFeedModeration initial(Integer feedId, String visibilityStatus) {
        AdminFeedModeration state = new AdminFeedModeration();
        state.feedId = feedId;
        state.visibilityStatus = visibilityStatus;
        state.recommended = false;
        return state;
    }

    public void changeVisibility(String visibilityStatus, String reason, Integer actorUserId) {
        this.visibilityStatus = visibilityStatus;
        touch(reason, actorUserId);
    }

    public void changeRecommendation(boolean recommended, String reason, Integer actorUserId) {
        this.recommended = recommended;
        touch(reason, actorUserId);
    }

    private void touch(String reason, Integer actorUserId) {
        this.reason = reason;
        this.moderatedBy = actorUserId;
        this.moderatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
