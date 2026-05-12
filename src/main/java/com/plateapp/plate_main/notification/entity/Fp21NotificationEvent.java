package com.plateapp.plate_main.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "fp_21")
public class Fp21NotificationEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Long eventId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "actor_user_id", nullable = false)
    private Integer actorUserId;

    @Column(name = "object_type", nullable = false, length = 50)
    private String objectType;

    @Column(name = "object_id", nullable = false)
    private Long objectId;

    @Column(name = "parent_object_type", length = 50)
    private String parentObjectType;

    @Column(name = "parent_object_id")
    private Long parentObjectId;

    @Column(name = "message_template", length = 100)
    private String messageTemplate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "message_params", columnDefinition = "jsonb")
    private Map<String, Object> messageParams;

    @Column(name = "dedupe_key", length = 200)
    private String dedupeKey;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getEventId() { return eventId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public Integer getActorUserId() { return actorUserId; }
    public void setActorUserId(Integer actorUserId) { this.actorUserId = actorUserId; }
    public String getObjectType() { return objectType; }
    public void setObjectType(String objectType) { this.objectType = objectType; }
    public Long getObjectId() { return objectId; }
    public void setObjectId(Long objectId) { this.objectId = objectId; }
    public String getParentObjectType() { return parentObjectType; }
    public void setParentObjectType(String parentObjectType) { this.parentObjectType = parentObjectType; }
    public Long getParentObjectId() { return parentObjectId; }
    public void setParentObjectId(Long parentObjectId) { this.parentObjectId = parentObjectId; }
    public String getMessageTemplate() { return messageTemplate; }
    public void setMessageTemplate(String messageTemplate) { this.messageTemplate = messageTemplate; }
    public Map<String, Object> getMessageParams() { return messageParams; }
    public void setMessageParams(Map<String, Object> messageParams) { this.messageParams = messageParams; }
    public String getDedupeKey() { return dedupeKey; }
    public void setDedupeKey(String dedupeKey) { this.dedupeKey = dedupeKey; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
