package com.plateapp.plate_main.comment.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "fp_460")
public class Fp460FeedComment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "comment_id")
  private Integer commentId;

  @Column(name = "feed_id", nullable = false)
  private Integer feedId;

  @Column(name = "username", nullable = false, length = 255)
  private String username;

  @Column(name = "content", nullable = false, columnDefinition = "text")
  private String content;

  @Column(name = "use_yn", nullable = false, columnDefinition = "bpchar(1)")
  private String useYn = "Y";

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  void prePersist() {
    LocalDateTime now = LocalDateTime.now();
    if (createdAt == null) createdAt = now;
    if (updatedAt == null) updatedAt = now;
    if (useYn == null) useYn = "Y";
    // deletedAt 은 기본 null 유지
  }

  @PreUpdate
  void preUpdate() {
    updatedAt = LocalDateTime.now();
  }

  public Integer getCommentId() { return commentId; }

  public Integer getFeedId() { return feedId; }
  public void setFeedId(Integer feedId) { this.feedId = feedId; }

  public String getUsername() { return username; }
  public void setUsername(String username) { this.username = username; }

  public String getContent() { return content; }
  public void setContent(String content) { this.content = content; }

  public String getUseYn() { return useYn; }
  public void setUseYn(String useYn) { this.useYn = useYn; }

  public LocalDateTime getDeletedAt() { return deletedAt; }
  public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

  public LocalDateTime getCreatedAt() { return createdAt; }
  public LocalDateTime getUpdatedAt() { return updatedAt; }
}
