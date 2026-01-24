package com.plateapp.plate_main.like.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "fp_50")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(StoreLike.StoreLikeId.class)
public class StoreLike {

    // fp_50: 비디오 피드(스토어) 좋아요
    @Id
    @Column(name = "store_id")
    private Integer storeId;

    @Id
    @Column(name = "username")
    private String userId; // 필드명 유지, username 저장

    @Column(name = "use_yn", length = 1)
    private String useYn; // Y=활성, N=비활성(소프트 삭제)

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (useYn == null) {
            useYn = "Y";
        }
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StoreLikeId implements Serializable {
        private Integer storeId;
        private String userId;
    }
}
