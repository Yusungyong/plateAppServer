package com.plateapp.plate_main.admin.outbox.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Map;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "admin_outbox_events")
@Getter
public class AdminOutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "aggregate_type", nullable = false, length = 80)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount;

    @Column(name = "available_at", nullable = false)
    private OffsetDateTime availableAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    protected AdminOutboxEvent() {
    }

    public static AdminOutboxEvent pending(
            String eventType,
            String aggregateType,
            String aggregateId,
            Map<String, Object> payload,
            OffsetDateTime now
    ) {
        AdminOutboxEvent event = new AdminOutboxEvent();
        event.eventType = eventType;
        event.aggregateType = aggregateType;
        event.aggregateId = aggregateId;
        event.payload = payload;
        event.status = "pending";
        event.attemptCount = 0;
        event.availableAt = now;
        event.createdAt = now;
        return event;
    }

    public void markProcessed(OffsetDateTime now) {
        this.status = "processed";
        this.processedAt = now;
        this.lastError = null;
    }

    public void markRetry(OffsetDateTime availableAt, String error) {
        this.attemptCount = this.attemptCount + 1;
        this.lastError = truncate(error);
        if (this.attemptCount >= 5) {
            this.status = "failed";
            return;
        }
        this.status = "pending";
        this.availableAt = availableAt;
    }

    private String truncate(String value) {
        if (value == null || value.length() <= 2000) {
            return value;
        }
        return value.substring(0, 2000);
    }
}
