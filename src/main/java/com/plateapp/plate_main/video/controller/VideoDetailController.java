package com.plateapp.plate_main.video.controller;

import com.plateapp.plate_main.common.api.ApiResponse;
import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import com.plateapp.plate_main.video.dto.VideoFeedItemDTO;
import com.plateapp.plate_main.video.service.VideoDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/videos")
public class VideoDetailController {

    private final VideoDetailService videoDetailService;

    @GetMapping("/{videoStoreId}")
    public ApiResponse<VideoFeedItemDTO> getVideo(
            @PathVariable Integer videoStoreId,
            Authentication authentication
    ) {
        return ApiResponse.ok(videoDetailService.getVideo(currentUsername(authentication), videoStoreId));
    }

    private String currentUsername(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.AUTH_UNAUTHORIZED);
        }

        String username = authentication.getName();
        if (username == null || username.isBlank() || "anonymousUser".equalsIgnoreCase(username)) {
            throw new AppException(ErrorCode.AUTH_UNAUTHORIZED);
        }
        return username;
    }
}
