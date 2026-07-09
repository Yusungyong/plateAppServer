package com.plateapp.plate_main.owner.controller;

import com.plateapp.plate_main.common.api.ApiResponse;
import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import com.plateapp.plate_main.owner.dto.OwnerStoreAnalyticsDtos;
import com.plateapp.plate_main.owner.service.OwnerStoreAnalyticsService;
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
@RequestMapping("/api/owner/stores/{storeId}/analytics")
@RequiredArgsConstructor
public class OwnerStoreAnalyticsController {

    private final OwnerStoreAnalyticsService analyticsService;

    @GetMapping("/summary")
    public ApiResponse<OwnerStoreAnalyticsDtos.SummaryResponse> summary(
            @PathVariable Long storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication authentication
    ) {
        return ApiResponse.ok(analyticsService.summary(username(authentication), storeId, from, to));
    }

    @GetMapping("/trends")
    public ApiResponse<OwnerStoreAnalyticsDtos.TrendResponse> trends(
            @PathVariable Long storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "day") String interval,
            Authentication authentication
    ) {
        return ApiResponse.ok(analyticsService.trends(username(authentication), storeId, from, to, interval));
    }

    @GetMapping("/contents")
    public ApiResponse<OwnerStoreAnalyticsDtos.ContentPerformanceResponse> contents(
            @PathVariable Long storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        return ApiResponse.ok(analyticsService.contents(username(authentication), storeId, from, to, page, size));
    }

    private String username(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new AppException(ErrorCode.AUTH_UNAUTHORIZED);
        }
        return authentication.getName();
    }
}
