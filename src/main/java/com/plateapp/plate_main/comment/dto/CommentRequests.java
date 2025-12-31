package com.plateapp.plate_main.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CommentRequests {
  public record CreateComment(@NotBlank @Size(max = 2000) String content) {}
  public record UpdateComment(@NotBlank @Size(max = 2000) String content) {}
  public record CreateReply(@NotBlank @Size(max = 2000) String content) {}
  public record UpdateReply(@NotBlank @Size(max = 2000) String content) {}
}
