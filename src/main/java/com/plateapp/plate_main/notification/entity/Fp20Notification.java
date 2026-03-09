package com.plateapp.plate_main.notification.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "fp_20")
public class Fp20Notification {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "receiver_id", nullable = false, length = 50)
  private String receiverId;

  @Column(name = "sender_id", length = 50)
  private String senderId;

  @Column(name = "type", nullable = false, length = 50)
  private String type;

  @Column(name = "message", nullable = false, columnDefinition = "text")
  private String message;

  @Column(name = "reference_id")
  private Long referenceId;

  @Column(name = "is_read")
  private Boolean isRead;

  @Column(name = "comment_id")
  private Long commentId;

  @Column(name = "reply_id")
  private Long replyId;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @PrePersist
  void prePersist() {
    LocalDateTime now = LocalDateTime.now();
    if (createdAt == null) createdAt = now;
    if (isRead == null) isRead = false;
  }

  public Long getId() { return id; }

  public String getReceiverId() { return receiverId; }
  public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

  public String getSenderId() { return senderId; }
  public void setSenderId(String senderId) { this.senderId = senderId; }

  public String getType() { return type; }
  public void setType(String type) { this.type = type; }

  public String getMessage() { return message; }
  public void setMessage(String message) { this.message = message; }

  public Long getReferenceId() { return referenceId; }
  public void setReferenceId(Long referenceId) { this.referenceId = referenceId; }

  public Boolean getIsRead() { return isRead; }
  public void setIsRead(Boolean isRead) { this.isRead = isRead; }

  public Long getCommentId() { return commentId; }
  public void setCommentId(Long commentId) { this.commentId = commentId; }

  public Long getReplyId() { return replyId; }
  public void setReplyId(Long replyId) { this.replyId = replyId; }

  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
