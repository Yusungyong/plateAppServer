package com.plateapp.plate_main.comment.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "fp_450")
public class Fp450Reply {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "reply_id")
  private Integer replyId;

  @Column(name = "content", nullable = false, columnDefinition = "text")
  private String content;

  @Column(name = "username", nullable = false, length = 50)
  private String username;

  @Column(name = "comment_id", nullable = false)
  private Integer commentId;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Column(name = "use_yn", nullable = false, length = 1)
  private String useYn = "Y";

  // fp_450.deleted_at 는 timestamp
  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @PrePersist
  void onCreate() {
    LocalDateTime now = LocalDateTime.now();
    if (createdAt == null) createdAt = now;
    if (updatedAt == null) updatedAt = now;
    if (useYn == null) useYn = "Y";
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = LocalDateTime.now();
  }

  // --- getters/setters ---
  public Integer getReplyId() { return replyId; }
  public void setReplyId(Integer replyId) { this.replyId = replyId; }

  public String getContent() { return content; }
  public void setContent(String content) { this.content = content; }

  public String getUsername() { return username; }
  public void setUsername(String username) { this.username = username; }

  public Integer getCommentId() { return commentId; }
  public void setCommentId(Integer commentId) { this.commentId = commentId; }

  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

  public String getUseYn() { return useYn; }
  public void setUseYn(String useYn) { this.useYn = useYn; }

  public LocalDateTime getDeletedAt() { return deletedAt; }
  public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
}
