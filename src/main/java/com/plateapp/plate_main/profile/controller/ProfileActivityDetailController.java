package com.plateapp.plate_main.profile.controller;

import com.plateapp.plate_main.common.dto.ApiResponse;
import com.plateapp.plate_main.profile.dto.LikedContentDtos.LikedImageItem;
import com.plateapp.plate_main.profile.dto.LikedContentDtos.LikedVideoItem;
import com.plateapp.plate_main.profile.dto.ProfileActivityDetailItems.ImageItem;
import com.plateapp.plate_main.profile.dto.ProfileActivityDetailItems.VideoItem;
import com.plateapp.plate_main.profile.dto.ProfileActivityDetailResponse;
import com.plateapp.plate_main.profile.service.ProfileActivityDetailService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ProfileActivityDetailController {

    private final ProfileActivityDetailService profileActivityDetailService;

    @GetMapping("/users/{username}/videos")
    public ApiResponse<ProfileActivityDetailResponse<VideoItem>> getUserVideos(
            @PathVariable String username,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "recent") String sort,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String region
    ) {
        return ApiResponse.success(
                profileActivityDetailService.getUserVideos(username, limit, offset, sort, from, to, region)
        );
    }

    @GetMapping("/users/{username}/images")
    public ApiResponse<ProfileActivityDetailResponse<ImageItem>> getUserImages(
            @PathVariable String username,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "recent") String sort,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String region
    ) {
        return ApiResponse.success(
                profileActivityDetailService.getUserImages(username, limit, offset, sort, from, to, region)
        );
    }

    @GetMapping("/users/{username}/likes/videos")
    public ApiResponse<ProfileActivityDetailResponse<LikedVideoItem>> getUserLikedVideos(
            @PathVariable String username,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "recent") String sort,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String region
    ) {
        return ApiResponse.success(
                profileActivityDetailService.getLikedVideos(username, limit, offset, sort, from, to, region)
        );
    }

    @GetMapping("/users/{username}/likes/images")
    public ApiResponse<ProfileActivityDetailResponse<LikedImageItem>> getUserLikedImages(
            @PathVariable String username,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "recent") String sort,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String region
    ) {
        return ApiResponse.success(
                profileActivityDetailService.getLikedImages(username, limit, offset, sort, from, to, region)
        );
    }

    @GetMapping("/my/videos")
    public ApiResponse<ProfileActivityDetailResponse<VideoItem>> getMyVideos(
            Authentication authentication,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "recent") String sort,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String region
    ) {
        String username = authentication.getName();
        return ApiResponse.success(
                profileActivityDetailService.getUserVideos(username, limit, offset, sort, from, to, region)
        );
    }

    @GetMapping("/my/images")
    public ApiResponse<ProfileActivityDetailResponse<ImageItem>> getMyImages(
            Authentication authentication,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "recent") String sort,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String region
    ) {
        String username = authentication.getName();
        return ApiResponse.success(
                profileActivityDetailService.getUserImages(username, limit, offset, sort, from, to, region)
        );
    }

    @GetMapping("/my/likes/videos")
    public ApiResponse<ProfileActivityDetailResponse<LikedVideoItem>> getMyLikedVideos(
            Authentication authentication,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "recent") String sort,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String region
    ) {
        String username = authentication.getName();
        return ApiResponse.success(
                profileActivityDetailService.getLikedVideos(username, limit, offset, sort, from, to, region)
        );
    }

    @GetMapping("/my/likes/images")
    public ApiResponse<ProfileActivityDetailResponse<LikedImageItem>> getMyLikedImages(
            Authentication authentication,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "recent") String sort,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String region
    ) {
        String username = authentication.getName();
        return ApiResponse.success(
                profileActivityDetailService.getLikedImages(username, limit, offset, sort, from, to, region)
        );
    }
}
