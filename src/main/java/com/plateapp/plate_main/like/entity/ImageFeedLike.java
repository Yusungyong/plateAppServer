package com.plateapp.plate_main.like.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "fp_60")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(ImageFeedLike.ImageFeedLikeId.class)
public class ImageFeedLike {

    // fp_60: 이미지 피드 좋아요
    @Id
    @Column(name = "feed_id")
    private Integer imageFeedId;

    @Id
    @Column(name = "username")
    private String userId; // 이름은 유지하지만 값은 username 사용

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
    public static class ImageFeedLikeId implements Serializable {
        private Integer imageFeedId;
        private String userId;
    }
}
