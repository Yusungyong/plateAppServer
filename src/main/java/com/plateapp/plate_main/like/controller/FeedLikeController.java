package com.plateapp.plate_main.like.controller;

import com.plateapp.plate_main.common.api.ApiResponse;
import com.plateapp.plate_main.like.dto.LikeResponses;
import com.plateapp.plate_main.like.dto.LikeUserResponse;
import com.plateapp.plate_main.like.service.FeedLikeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/image-feeds")
public class FeedLikeController {

  private final FeedLikeService service;

  public FeedLikeController(FeedLikeService service) {
    this.service = service;
  }

  // POST 토글
  @PostMapping("/{feedId}/likes/toggle")
  public ApiResponse<LikeResponses.ToggleLikeResponse> toggleLike(
      @PathVariable("feedId") Integer feedId
  ) {
    return ApiResponse.ok(service.toggleLike(feedId));
  }

  // feed 좋아요 사용자 목록 조회
  @GetMapping("/{feedId}/likes/users")
  public ApiResponse<java.util.List<LikeUserResponse>> likeUsers(
          @PathVariable("feedId") Integer feedId,
          @RequestParam(name = "limit", defaultValue = "20") int limit,
          @RequestParam(name = "offset", defaultValue = "0") int offset
  ) {
    return ApiResponse.ok(service.findLikeUsers(feedId, limit, offset));
  }
}
