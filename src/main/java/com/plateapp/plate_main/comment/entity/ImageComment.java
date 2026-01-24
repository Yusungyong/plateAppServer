package com.plateapp.plate_main.comment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "fp_440")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImageComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Integer commentId;

    // 실제 테이블에는 image_feed_id 컬럼이 없고 store_id 를 사용하고 있으므로
    // 컬럼명을 store_id 로 매핑한다.
    @Column(name = "store_id", nullable = false)
    private Integer imageFeedId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "username")
    private String username;

    @Column(name = "content", nullable = false, length = 500)
    private String content;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "use_yn", length = 1)
    @Builder.Default
    private String useYn = "Y";

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (useYn == null) {
            useYn = "Y";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
