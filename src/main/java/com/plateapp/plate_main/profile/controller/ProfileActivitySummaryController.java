package com.plateapp.plate_main.profile.controller;

import com.plateapp.plate_main.common.dto.ApiResponse;
import com.plateapp.plate_main.profile.dto.ProfileActivitySummaryResponse;
import com.plateapp.plate_main.profile.service.ProfileActivitySummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ProfileActivitySummaryController {

    private final ProfileActivitySummaryService profileActivitySummaryService;

    @GetMapping("/users/{username}/activity-summary")
    public ApiResponse<ProfileActivitySummaryResponse> getPublicActivitySummary(@PathVariable String username) {
        return ApiResponse.success(profileActivitySummaryService.getPublicSummary(username));
    }

    @GetMapping("/my/activity-summary")
    public ApiResponse<ProfileActivitySummaryResponse> getMyActivitySummary(Authentication authentication) {
        String username = authentication.getName();
        return ApiResponse.success(profileActivitySummaryService.getMySummary(username));
    }
}
