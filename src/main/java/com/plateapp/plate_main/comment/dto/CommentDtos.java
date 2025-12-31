package com.plateapp.plate_main.comment.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CommentDtos {

  // ===== Requests =====
  public static class CreateCommentRequest {
    public String content;
  }

  public static class UpdateCommentRequest {
    public String content;
  }

  public static class CreateReplyRequest {
    public String content;
  }

  public static class UpdateReplyRequest {
    public String content;
  }

  // ===== User Profile in Response =====
  public static class UserProfile {
    public String username;
    public Integer userId;
    public String nickName;
    public String profileImageUrl;
    public Boolean isPrivate;
  }

  // ===== Responses =====
  public static class ReplyResponse {
    public Integer replyId;
    public Integer commentId;
    public String content;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;

    public UserProfile author;
  }

  public static class CommentResponse {
    public Integer commentId;
    public Integer storeId;
    public String content;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;

    public UserProfile author;

    // ✅ B안: 댓글 목록에서는 replies 대신 replyCount만 내려줌
    public long replyCount;

    // (유지) 클라 호환용: 댓글 목록에서는 비어있게 내려감
    public List<ReplyResponse> replies = new ArrayList<>();
  }

  public static class PageResponse<T> {
    public int page;
    public int size;
    public long total;
    public List<T> items;

    public PageResponse(int page, int size, long total, List<T> items) {
      this.page = page;
      this.size = size;
      this.total = total;
      this.items = items;
    }
  }
}
