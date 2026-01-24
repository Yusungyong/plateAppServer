package com.plateapp.plate_main.like.controller;

import com.plateapp.plate_main.common.dto.ApiResponse;
import com.plateapp.plate_main.common.dto.PagedResponse;
import com.plateapp.plate_main.like.dto.LikeStatusResponse;
import com.plateapp.plate_main.like.dto.LikeToggleResponse;
import com.plateapp.plate_main.like.dto.LikeUserDTO;
import com.plateapp.plate_main.like.service.StoreLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stores")
@RequiredArgsConstructor
public class StoreLikeController {

    private final StoreLikeService likeService;

    @PostMapping("/{storeId}/likes/toggle")
    public ApiResponse<LikeToggleResponse> toggleLike(
            @PathVariable Integer storeId,
            Authentication authentication) {
        String username = authentication.getName();
        LikeToggleResponse response = likeService.toggleLike(storeId, username);
        return ApiResponse.success(response);
    }

    @GetMapping("/{storeId}/likes/status")
    public ApiResponse<LikeStatusResponse> getLikeStatus(
            @PathVariable Integer storeId,
            Authentication authentication) {
        String username = authentication.getName();
        LikeStatusResponse response = likeService.getLikeStatus(storeId, username);
        return ApiResponse.success(response);
    }

    @GetMapping("/{storeId}/likes/users")
    public ApiResponse<PagedResponse<LikeUserDTO>> getLikeUsers(
            @PathVariable Integer storeId,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        PagedResponse<LikeUserDTO> response = likeService.getLikeUsers(storeId, limit, offset);
        return ApiResponse.success(response);
    }
}
