package com.plateapp.plate_main.contentanalytics.controller;

import com.plateapp.plate_main.common.api.ApiResponse;
import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import com.plateapp.plate_main.contentanalytics.dto.ContentAnalyticsDtos;
import com.plateapp.plate_main.contentanalytics.service.ContentAnalyticsService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/my/content-analytics")
@RequiredArgsConstructor
public class ContentAnalyticsController {

    private final ContentAnalyticsService contentAnalyticsService;

    @GetMapping("/summary")
    public ApiResponse<ContentAnalyticsDtos.SummaryResponse> summary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication authentication
    ) {
        return ApiResponse.ok(contentAnalyticsService.summary(currentUsername(authentication), from, to));
    }

    @GetMapping("/trends")
    public ApiResponse<ContentAnalyticsDtos.TrendsResponse> trends(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "day")
            @Pattern(regexp = "day", message = "interval must be day") String interval,
            Authentication authentication
    ) {
        return ApiResponse.ok(contentAnalyticsService.trends(
                currentUsername(authentication), from, to, interval));
    }

    @GetMapping("/contents")
    public ApiResponse<ContentAnalyticsDtos.ContentPageResponse> contents(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "all")
            @Pattern(regexp = "all|video|image", message = "type must be all, video, or image") String type,
            @RequestParam(defaultValue = "impressions")
            @Pattern(
                    regexp = "impressions|views|likes|comments|recent",
                    message = "sort must be impressions, views, likes, comments, or recent"
            ) String sort,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            Authentication authentication
    ) {
        return ApiResponse.ok(contentAnalyticsService.contents(
                currentUsername(authentication), from, to, type, sort, page, size));
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
