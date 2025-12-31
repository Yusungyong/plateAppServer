package com.plateapp.plate_main.comment.dto;

import java.time.LocalDateTime;
import java.util.List;

public class CommentResponses {

  public record ReplyDto(
      Integer replyId,
      Integer commentId,
      String username,
      String content,
      LocalDateTime createdAt,
      LocalDateTime updatedAt
  ) {}

  public record CommentDto(
      Integer commentId,
      Integer feedId,
      String username,
      String content,
      LocalDateTime createdAt,
      LocalDateTime updatedAt,
      long replyCount,
      List<ReplyDto> replies
  ) {}

  public record PageResponse<T>(
      List<T> content,
      long totalElements,
      int totalPages,
      int size,
      int number
  ) {}
}
