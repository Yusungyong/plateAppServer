package com.plateapp.plate_main.comment.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FeedCommentDtos {

  // ===== requests =====
  public static class CreateRequest {
    public String content;
  }

  public static class UpdateRequest {
    public String content;
  }

  // ===== common =====
  public static class UserProfile {
    public String username;
    public Integer userId;
    public String nickName;
    public String profileImageUrl;
    public Boolean isPrivate;
  }

  // ===== responses =====
  public static class CommentResponse {
    public Integer commentId;
    public Integer feedId;
    public String content;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;

    public UserProfile author;

    // ✅ B안: 댓글 목록은 replies 미포함 + replyCount만
    public long replyCount = 0;

    // 프론트 호환 위해 넣어도 되지만(항상 빈 배열), Video쪽과 완전 동일하게 가려면 제거해도 OK
    public List<ReplyResponse> replies = new ArrayList<>();
  }

  public static class ReplyResponse {
    public Integer replyId;
    public Integer commentId;
    public String content;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;

    public UserProfile author;
  }

  public static class PageResponse<T> {
    public int page;
    public int size;
    public long totalElements;
    public List<T> content;

    public PageResponse(int page, int size, long totalElements, List<T> content) {
      this.page = page;
      this.size = size;
      this.totalElements = totalElements;
      this.content = content;
    }
  }
}
