// src/main/java/com/plateapp/plate_main/feed/controller/ImageFeedController.java
package com.plateapp.plate_main.feed.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.plateapp.plate_main.common.api.ApiResponse;
import com.plateapp.plate_main.feed.dto.ImageFeedContextResponse;
import com.plateapp.plate_main.feed.dto.ImageFeedViewerResponse;
import com.plateapp.plate_main.feed.service.ImageFeedService;

@RestController
public class ImageFeedController {

  private final ImageFeedService imageFeedService;

  public ImageFeedController(ImageFeedService imageFeedService) {
    this.imageFeedService = imageFeedService;
  }

  @GetMapping("/image-feeds/{feedId}")
  public ApiResponse<ImageFeedViewerResponse> getImageFeedViewer(
          @PathVariable("feedId") Integer feedId,
          @RequestParam(name = "username", required = false) String username
  ) {
    return ApiResponse.ok(imageFeedService.getViewer(feedId, resolveUsername(username)));
  }

  // ✅ 추가: 선택한 피드 기준 위치 반경 내 피드ID 스트립
  @GetMapping("/image-feeds/context")
  public ApiResponse<ImageFeedContextResponse> getImageFeedContext(
      @RequestParam("baseFeedId") Integer baseFeedId,
      @RequestParam(value = "radiusM", required = false, defaultValue = "3000") Integer radiusM,
      @RequestParam(value = "limit", required = false, defaultValue = "50") Integer limit,
      @RequestParam(name = "username", required = false) String username
  ) {
    return ApiResponse.ok(imageFeedService.getContext(baseFeedId, radiusM, limit, resolveUsername(username)));
  }

  private String resolveUsername(String usernameParam) {
    if (usernameParam != null && !usernameParam.isBlank()) {
      return usernameParam;
    }
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.isAuthenticated()) {
      String name = auth.getName();
      if (name != null && !name.isBlank() && !"anonymousUser".equalsIgnoreCase(name)) {
        return name;
      }
    }
    return null;
  }
}
