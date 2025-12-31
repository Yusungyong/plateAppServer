package com.plateapp.plate_main.comment.controller;

import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.plateapp.plate_main.comment.dto.CommentDtos;
import com.plateapp.plate_main.comment.service.CommentService;
import com.plateapp.plate_main.common.api.ApiResponse;
import com.plateapp.plate_main.common.error.ErrorCode;
import com.plateapp.plate_main.user.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class StoreCommentController {

  private final CommentService commentService;
  private final MemberRepository memberRepository;

  private String currentUsername() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || auth.getName() == null || auth.getName().isBlank()) {
      throw new IllegalStateException("Unauthenticated");
    }
    return auth.getName();
  }

  // ✅ 댓글 목록 (로그인 불필요)
  @GetMapping("/stores/{storeId}/comments")
  public ResponseEntity<ApiResponse<CommentDtos.PageResponse<CommentDtos.CommentResponse>>> getStoreComments(
      @PathVariable("storeId") int storeId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size
  ) {
    return ResponseEntity.ok(ApiResponse.ok(commentService.getStoreComments(storeId, page, size)));
  }

  // ✅ 답글 목록 (로그인 불필요)
  @GetMapping("/comments/{commentId}/replies")
  public ResponseEntity<ApiResponse<CommentDtos.PageResponse<CommentDtos.ReplyResponse>>> getCommentReplies(
      @PathVariable("commentId") int commentId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "50") int size
  ) {
    return ResponseEntity.ok(ApiResponse.ok(commentService.getCommentReplies(commentId, page, size)));
  }

  // ✅ 댓글 작성 (인증 필요)
  @PostMapping("/stores/{storeId}/comments")
  public ResponseEntity<ApiResponse<Map<String, Object>>> addComment(
      @PathVariable("storeId") int storeId,
      @RequestBody CommentDtos.CreateCommentRequest body
  ) {
    String username = currentUsername();
    Integer userId = memberRepository.findById(username).map(u -> u.getUserId()).orElse(null);

    int commentId = commentService.addComment(storeId, username, userId, body.content);
    return ResponseEntity.ok(ApiResponse.ok(Map.of("commentId", commentId)));
  }

  @PutMapping("/comments/{commentId}")
  public ResponseEntity<ApiResponse<Map<String, Object>>> updateComment(
      @PathVariable("commentId") int commentId,
      @RequestBody CommentDtos.UpdateCommentRequest body
  ) {
    String username = currentUsername();
    commentService.updateComment(commentId, username, body.content);
    return ResponseEntity.ok(ApiResponse.ok(Map.of("ok", true)));
  }

  @DeleteMapping("/comments/{commentId}")
  public ResponseEntity<ApiResponse<Map<String, Object>>> deleteComment(
      @PathVariable("commentId") int commentId
  ) {
    String username = currentUsername();
    commentService.deleteComment(commentId, username);
    return ResponseEntity.ok(ApiResponse.ok(Map.of("ok", true)));
  }

  @PostMapping("/comments/{commentId}/replies")
  public ResponseEntity<ApiResponse<Map<String, Object>>> addReply(
      @PathVariable("commentId") int commentId,
      @RequestBody CommentDtos.CreateReplyRequest body
  ) {
    String username = currentUsername();
    int replyId = commentService.addReply(commentId, username, body.content);
    return ResponseEntity.ok(ApiResponse.ok(Map.of("replyId", replyId)));
  }

  @PutMapping("/replies/{replyId}")
  public ResponseEntity<ApiResponse<Map<String, Object>>> updateReply(
      @PathVariable("replyId") int replyId,
      @RequestBody CommentDtos.UpdateReplyRequest body
  ) {
    String username = currentUsername();
    commentService.updateReply(replyId, username, body.content);
    return ResponseEntity.ok(ApiResponse.ok(Map.of("ok", true)));
  }

  @DeleteMapping("/replies/{replyId}")
  public ResponseEntity<ApiResponse<Map<String, Object>>> deleteReply(
      @PathVariable("replyId") int replyId
  ) {
    String username = currentUsername();
    commentService.deleteReply(replyId, username);
    return ResponseEntity.ok(ApiResponse.ok(Map.of("ok", true)));
  }

  // ✅ 찾을 수 없음 처리 (컨트롤러에서 ApiResponse 형태로 통일)
  @ExceptionHandler(NoSuchElementException.class)
  public ResponseEntity<ApiResponse<Void>> handleNotFound(NoSuchElementException e) {
    ErrorCode ec = ErrorCode.COMMON_NOT_FOUND;
    return ResponseEntity.status(ec.getStatus())
        .body(ApiResponse.fail(ec.getCode(), ec.getDefaultMessage()));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiResponse<Void>> handleBadRequest(IllegalArgumentException e) {
    ErrorCode ec = ErrorCode.COMMON_INVALID_INPUT;
    return ResponseEntity.status(ec.getStatus())
        .body(ApiResponse.fail(ec.getCode(), e.getMessage()));
  }
}
