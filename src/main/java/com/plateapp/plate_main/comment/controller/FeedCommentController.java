package com.plateapp.plate_main.comment.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.plateapp.plate_main.comment.dto.FeedCommentDtos;
import com.plateapp.plate_main.comment.service.FeedCommentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/image-feeds")
public class FeedCommentController {

  private final FeedCommentService feedCommentService;

  private String currentUsername() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || auth.getName() == null || auth.getName().isBlank()) {
      throw new IllegalStateException("Unauthenticated");
    }
    return auth.getName();
  }

  // ✅ 댓글 목록 (B안: replyCount만)
  @GetMapping("/{feedId}/comments")
  public ResponseEntity<?> getFeedComments(
      @PathVariable("feedId") int feedId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size
  ) {
    return ResponseEntity.ok(feedCommentService.getFeedComments(feedId, page, size));
  }

  // ✅ 댓글 작성
  @PostMapping("/{feedId}/comments")
  public ResponseEntity<?> addComment(@PathVariable("feedId") int feedId, @RequestBody FeedCommentDtos.CreateRequest req) {
    String username = currentUsername();
    int commentId = feedCommentService.addComment(feedId, username, req.content);
    return ResponseEntity.ok(java.util.Map.of("commentId", commentId));
  }

  // ✅ 댓글 수정
  @PutMapping("/comments/{commentId}")
  public ResponseEntity<?> updateComment(@PathVariable("commentId") int commentId, @RequestBody FeedCommentDtos.UpdateRequest req) {
    String username = currentUsername();
    feedCommentService.updateComment(commentId, username, req.content);
    return ResponseEntity.ok(java.util.Map.of("ok", true));
  }

  // ✅ 댓글 삭제 (물리삭제)
  @DeleteMapping("/comments/{commentId}")
  public ResponseEntity<?> deleteComment(@PathVariable("commentId") int commentId) {
    String username = currentUsername();
    feedCommentService.deleteComment(commentId, username);
    return ResponseEntity.ok(java.util.Map.of("ok", true));
  }

  // ✅ 대댓글 목록 (펼칠 때만)
  @GetMapping("/comments/{commentId}/replies")
  public ResponseEntity<?> getReplies(
      @PathVariable("commentId") int commentId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size
  ) {
    return ResponseEntity.ok(feedCommentService.getCommentReplies(commentId, page, size));
  }

  // ✅ 대댓글 작성
  @PostMapping("/comments/{commentId}/replies")
  public ResponseEntity<?> addReply(@PathVariable("commentId") int commentId, @RequestBody FeedCommentDtos.CreateRequest req) {
    String username = currentUsername();
    int replyId = feedCommentService.addReply(commentId, username, req.content);
    return ResponseEntity.ok(java.util.Map.of("replyId", replyId));
  }

  // ✅ 대댓글 수정
  @PutMapping("/replies/{replyId}")
  public ResponseEntity<?> updateReply(@PathVariable("replyId") int replyId, @RequestBody FeedCommentDtos.UpdateRequest req) {
    String username = currentUsername();
    feedCommentService.updateReply(replyId, username, req.content);
    return ResponseEntity.ok(java.util.Map.of("ok", true));
  }

  // ✅ 대댓글 삭제 (물리삭제)
  @DeleteMapping("/replies/{replyId}")
  public ResponseEntity<?> deleteReply(@PathVariable("replyId") int replyId) {
    String username = currentUsername();
    feedCommentService.deleteReply(replyId, username);
    return ResponseEntity.ok(java.util.Map.of("ok", true));
  }
}
