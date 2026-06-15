package com.plateapp.plate_main.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;

@Entity
@Table(name = "admin_user_permissions")
@Getter
public class AdminUserPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "permission", nullable = false, length = 80)
    private String permission;

    @Column(name = "granted_by")
    private Integer grantedBy;

    @Column(name = "granted_at", nullable = false)
    private OffsetDateTime grantedAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    protected AdminUserPermission() {
    }
}
