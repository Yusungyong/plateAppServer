package com.plateapp.plate_main.like.controller;

import com.plateapp.plate_main.like.dto.LikeResponses;
import com.plateapp.plate_main.like.service.FeedLikeService;
import org.springframework.web.bind.annotation.*;

// ✅ 너 프로젝트 ApiResponse 경로로 수정
import com.plateapp.plate_main.common.api.ApiResponse;

@RestController
@RequestMapping("/image-feeds")
public class FeedLikeController {

  private final FeedLikeService service;

  public FeedLikeController(FeedLikeService service) {
    this.service = service;
  }

  // POST 토글 하나로 통일
  @PostMapping("/{feedId}/likes/toggle")
  public ApiResponse<LikeResponses.ToggleLikeResponse> toggleLike(
      @PathVariable("feedId") Integer feedId
  ) {
    return ApiResponse.ok(service.toggleLike(feedId));
  }
}
