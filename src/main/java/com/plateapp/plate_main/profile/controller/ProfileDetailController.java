package com.plateapp.plate_main.profile.controller;

import com.plateapp.plate_main.common.dto.ApiResponse;
import com.plateapp.plate_main.profile.dto.ProfileDetailResponse;
import com.plateapp.plate_main.profile.service.ProfileDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ProfileDetailController {

    private final ProfileDetailService profileDetailService;

    @GetMapping("/users/{username}/profile-detail")
    public ApiResponse<ProfileDetailResponse> getPublicProfileDetail(@PathVariable String username) {
        return ApiResponse.success(profileDetailService.getPublicProfileDetail(username));
    }

    @GetMapping("/my/profile-detail")
    public ApiResponse<ProfileDetailResponse> getMyProfileDetail(Authentication authentication) {
        String username = authentication.getName();
        return ApiResponse.success(profileDetailService.getMyProfileDetail(username));
    }
}
