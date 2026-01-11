package com.plateapp.plate_main.like.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.plateapp.plate_main.common.api.ApiResponse;
import com.plateapp.plate_main.like.dto.LikeUserResponse;
import com.plateapp.plate_main.like.service.LikeService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/likes")
public class LikeController {

  private final LikeService likeService;

  private String currentUsername() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || auth.getName() == null || auth.getName().isBlank()) {
      // 여기로 오는 경우는 거의 없음 (보통 Security가 먼저 401 처리)
      throw new IllegalStateException("Unauthenticated");
    }
    return auth.getName();
  }

  // ✅ 좋아요 누르기(없으면 생성, 있으면 Y로 활성화)
  @PostMapping("/{storeId}")
  public ResponseEntity<ApiResponse<Map<String, Object>>> like(
      @PathVariable("storeId") Integer storeId
  ) {
    String username = currentUsername();
    likeService.like(username, storeId);
    return ResponseEntity.ok(ApiResponse.ok(Map.of("liked", true)));
  }

  // ✅ 좋아요 취소(use_yn = N, deleted_at 찍기)
  @DeleteMapping("/{storeId}")
  public ResponseEntity<ApiResponse<Map<String, Object>>> unlike(
      @PathVariable("storeId") Integer storeId
  ) {
    String username = currentUsername();
    likeService.unlike(username, storeId);
    return ResponseEntity.ok(ApiResponse.ok(Map.of("liked", false)));
  }

  // ✅ 내가 이 매장을 좋아요 했는지
  @GetMapping("/{storeId}/me")
  public ResponseEntity<ApiResponse<Map<String, Object>>> isLiked(
      @PathVariable("storeId") Integer storeId
  ) {
    String username = currentUsername();
    boolean liked = likeService.isLiked(username, storeId);
    return ResponseEntity.ok(ApiResponse.ok(Map.of("liked", liked)));
  }

  // ✅ 매장 좋아요 수 (로그인 불필요)
  @GetMapping("/{storeId}/count")
  public ResponseEntity<ApiResponse<Map<String, Object>>> count(
      @PathVariable("storeId") Integer storeId
  ) {
    long count = likeService.countLikes(storeId);
    return ResponseEntity.ok(ApiResponse.ok(Map.of("count", count)));
  }

  // ✅ 내가 좋아요한 storeId 리스트
  @GetMapping("/me")
  public ResponseEntity<ApiResponse<Map<String, Object>>> myLikes() {
    String username = currentUsername();
    return ResponseEntity.ok(ApiResponse.ok(Map.of("storeIds", likeService.myLikedStoreIds(username))));
  }

  // storeId 좋아요한 사용자 목록 조회 (프로필/활동지역 포함)
  @GetMapping("/{storeId}/users")
  public ResponseEntity<ApiResponse<java.util.List<LikeUserResponse>>> likeUsers(
          @PathVariable("storeId") Integer storeId,
          @RequestParam(name = "limit", defaultValue = "20") int limit,
          @RequestParam(name = "offset", defaultValue = "0") int offset
  ) {
    return ResponseEntity.ok(ApiResponse.ok(likeService.findLikeUsers(storeId, limit, offset)));
  }
}
