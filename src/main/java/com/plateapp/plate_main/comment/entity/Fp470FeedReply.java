package com.plateapp.plate_main.comment.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "fp_470")
public class Fp470FeedReply {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "reply_id")
  private Integer replyId;

  @Column(name = "comment_id", nullable = false)
  private Integer commentId;

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
  }

  @PreUpdate
  void preUpdate() {
    updatedAt = LocalDateTime.now();
  }

  public Integer getReplyId() { return replyId; }

  public Integer getCommentId() { return commentId; }
  public void setCommentId(Integer commentId) { this.commentId = commentId; }

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
