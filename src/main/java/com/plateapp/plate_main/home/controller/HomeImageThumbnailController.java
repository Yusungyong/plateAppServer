package com.plateapp.plate_main.home.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
      @RequestParam(name = "size", defaultValue = "4") int size
  ) {
    return ApiResponse.ok(homeService.getLatestThumbs(size));
  }

}
