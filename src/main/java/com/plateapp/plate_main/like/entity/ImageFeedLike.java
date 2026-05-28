package com.plateapp.plate_main.like.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "fp_60")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(ImageFeedLike.ImageFeedLikeId.class)
public class ImageFeedLike {

    @Id
    @Column(name = "feed_id")
    private Integer imageFeedId;

    @Id
    @Column(name = "username")
    private String userId;

    @Column(name = "use_yn", length = 1)
    @JdbcTypeCode(SqlTypes.CHAR)
    private String useYn;

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
