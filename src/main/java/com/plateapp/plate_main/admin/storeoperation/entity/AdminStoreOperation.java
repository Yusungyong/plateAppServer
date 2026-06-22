package com.plateapp.plate_main.admin.storeoperation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.Getter;

@Entity
@Table(name = "admin_store_operations")
@Getter
public class AdminStoreOperation {
    @Id
    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "operation_status", nullable = false, length = 30)
    private String operationStatus;

    @Column(name = "visibility_status", nullable = false, length = 30)
    private String visibilityStatus;

    @Column(name = "reason", columnDefinition = "text")
    private String reason;

    @Column(name = "updated_by")
    private Integer updatedBy;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Version
    private Long version = 0L;

    protected AdminStoreOperation() {}

    public static AdminStoreOperation initial(Long storeId, String visibilityStatus) {
        AdminStoreOperation state = new AdminStoreOperation();
        state.storeId = storeId;
        state.operationStatus = "operating";
        state.visibilityStatus = visibilityStatus;
        return state;
    }

    public void changeOperation(String status, String reason, Integer actorUserId) {
        this.operationStatus = status;
        touch(reason, actorUserId);
    }

    public void changeVisibility(String status, String reason, Integer actorUserId) {
        this.visibilityStatus = status;
        touch(reason, actorUserId);
    }

    private void touch(String reason, Integer actorUserId) {
        this.reason = reason;
        this.updatedBy = actorUserId;
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
