package com.plateapp.plate_main.home.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.plateapp.plate_main.common.api.ApiResponse;
import com.plateapp.plate_main.home.dto.HomeImageThumbnailResponse;
import com.plateapp.plate_main.home.service.HomeImageThumbnailService;

@RestController
@RequestMapping("/api/home")
public class HomeImageThumbnailController {

  private final HomeImageThumbnailService homeService;

  public HomeImageThumbnailController(HomeImageThumbnailService homeService) {
    this.homeService = homeService;
  }

  @GetMapping("/image-thumbnails")
  public ApiResponse<HomeImageThumbnailResponse> getHomeImageThumbnails(
      @RequestParam(name = "size", defaultValue = "6") int size,
      @RequestParam(name = "sortType", defaultValue = "RECENT") String sortType,
      @RequestParam(name = "lat", required = false) Double lat,
      @RequestParam(name = "lng", required = false) Double lng,
      @RequestParam(name = "radius", required = false) Double radius,
      @RequestParam(name = "username", required = false) String username
  ) {
    return ApiResponse.ok(homeService.getLatestThumbs(size, sortType, lat, lng, radius, resolveUsername(username)));
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
