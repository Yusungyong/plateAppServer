package com.plateapp.plate_main.admin.audit.entity;

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
@Table(name = "admin_audit_logs")
@Getter
public class AdminAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    @Column(name = "actor_user_id", nullable = false)
    private Integer actorUserId;

    @Column(name = "actor_role", nullable = false, length = 50)
    private String actorRole;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "resource_type", nullable = false, length = 80)
    private String resourceType;

    @Column(name = "resource_id", nullable = false, length = 100)
    private String resourceId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "previous_value", columnDefinition = "jsonb")
    private Map<String, Object> previousValue;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "next_value", columnDefinition = "jsonb")
    private Map<String, Object> nextValue;

    @Column(name = "reason_code", length = 80)
    private String reasonCode;

    @Column(name = "reason", columnDefinition = "text")
    private String reason;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "request_id", length = 100)
    private String requestId;

    protected AdminAuditLog() {
    }

    public static AdminAuditLog create(
            OffsetDateTime occurredAt,
            Integer actorUserId,
            String actorRole,
            String action,
            String resourceType,
            String resourceId,
            Map<String, Object> previousValue,
            Map<String, Object> nextValue,
            String reasonCode,
            String reason,
            String ipAddress,
            String userAgent,
            String requestId
    ) {
        AdminAuditLog log = new AdminAuditLog();
        log.occurredAt = occurredAt;
        log.actorUserId = actorUserId;
        log.actorRole = actorRole;
        log.action = action;
        log.resourceType = resourceType;
        log.resourceId = resourceId;
        log.previousValue = previousValue;
        log.nextValue = nextValue;
        log.reasonCode = reasonCode;
        log.reason = reason;
        log.ipAddress = ipAddress;
        log.userAgent = userAgent;
        log.requestId = requestId;
        return log;
    }
}
