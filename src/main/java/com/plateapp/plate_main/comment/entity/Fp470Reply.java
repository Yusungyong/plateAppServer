// src/main/java/com/plateapp/plate_main/comment/entity/Fp470Reply.java
package com.plateapp.plate_main.comment.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "fp_470")
public class Fp470Reply {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "reply_id")
  private Integer replyId;

  @Column(name = "comment_id", nullable = false)
  private Integer commentId;

  @Column(name = "username", nullable = false)
  private String username;

  @Column(name = "content", nullable = false, columnDefinition = "text")
  private String content;

  @Column(name = "use_yn", nullable = false, columnDefinition = "bpchar(1)")
  @JdbcTypeCode(SqlTypes.CHAR)
  private String useYn = "Y";

  @Column(name = "deleted_at")
  private LocalDateTime deletedAt;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  public Integer getReplyId() { return replyId; }
  public Integer getCommentId() { return commentId; }
  public String getUseYn() { return useYn; }
  public LocalDateTime getDeletedAt() { return deletedAt; }
}
