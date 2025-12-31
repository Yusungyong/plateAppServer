package com.plateapp.plate_main.comment.entity;

import java.time.LocalDate;
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
@Table(name = "fp_440")
public class Fp440Comment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "comment_id")
  private Integer commentId;

  @Column(name = "store_id", nullable = false)
  private Integer storeId;

  @Column(name = "username", nullable = false, length = 255)
  private String username;

  @Column(name = "content", nullable = false, columnDefinition = "text")
  private String content;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Column(name = "use_yn", nullable = false, length = 1)
  private String useYn = "Y";

  // fp_440.deleted_at 는 date
  @Column(name = "deleted_at")
  private LocalDate deletedAt;

  @Column(name = "user_id")
  private Integer userId;

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
  public Integer getCommentId() { return commentId; }
  public void setCommentId(Integer commentId) { this.commentId = commentId; }

  public Integer getStoreId() { return storeId; }
  public void setStoreId(Integer storeId) { this.storeId = storeId; }

  public String getUsername() { return username; }
  public void setUsername(String username) { this.username = username; }

  public String getContent() { return content; }
  public void setContent(String content) { this.content = content; }

  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

  public LocalDateTime getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

  public String getUseYn() { return useYn; }
  public void setUseYn(String useYn) { this.useYn = useYn; }

  public LocalDate getDeletedAt() { return deletedAt; }
  public void setDeletedAt(LocalDate deletedAt) { this.deletedAt = deletedAt; }

  public Integer getUserId() { return userId; }
  public void setUserId(Integer userId) { this.userId = userId; }
}
