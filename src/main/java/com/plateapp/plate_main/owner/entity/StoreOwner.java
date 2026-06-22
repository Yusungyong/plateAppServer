package com.plateapp.plate_main.owner.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.Getter;

@Entity
@Table(name = "store_owners")
@Getter
public class StoreOwner {

    public static final String ROLE_OWNER = "OWNER";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "owner_role", nullable = false, length = 30)
    private String ownerRole;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    protected StoreOwner() {
    }

    public static StoreOwner createOwner(Long storeId, Integer userId) {
        StoreOwner owner = new StoreOwner();
        owner.storeId = storeId;
        owner.userId = userId;
        owner.ownerRole = ROLE_OWNER;
        return owner;
    }

    public void revoke(OffsetDateTime now) {
        this.revokedAt = now;
    }

    public void restore() {
        this.revokedAt = null;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
