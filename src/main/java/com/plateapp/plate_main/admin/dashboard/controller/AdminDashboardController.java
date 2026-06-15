package com.plateapp.plate_main.admin.dashboard.controller;

import com.plateapp.plate_main.admin.dashboard.dto.AdminDashboardDtos;
import com.plateapp.plate_main.admin.dashboard.service.AdminDashboardService;
import com.plateapp.plate_main.auth.security.PlateAuthorities;
import com.plateapp.plate_main.common.api.ApiResponse;
import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    @GetMapping("/dashboard/summary")
    public ApiResponse<AdminDashboardDtos.SummaryResponse> summary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication authentication
    ) {
        requireDashboardRead(authentication);
        return ApiResponse.ok(dashboardService.summary(from, to));
    }

    @GetMapping("/dashboard/activity-trends")
    public ApiResponse<List<AdminDashboardDtos.ActivityTrend>> trends(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "day") String interval,
            Authentication authentication
    ) {
        requireDashboardRead(authentication);
        return ApiResponse.ok(dashboardService.trends(from, to, interval));
    }

    @GetMapping("/dashboard/region-distribution")
    public ApiResponse<List<AdminDashboardDtos.RegionDistribution>> regionDistribution(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication authentication
    ) {
        requireDashboardRead(authentication);
        return ApiResponse.ok(dashboardService.regionDistribution(from, to));
    }

    @GetMapping("/activities")
    public ApiResponse<AdminDashboardDtos.ActivityListResponse> activities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "occurredAt,desc") String sort,
            Authentication authentication
    ) {
        requireDashboardRead(authentication);
        return ApiResponse.ok(dashboardService.activities(page, size, sort));
    }

    private void requireDashboardRead(Authentication authentication) {
        if (!PlateAuthorities.hasAdminPermission(authentication, PlateAuthorities.PERMISSION_DASHBOARD_READ)) {
            throw new AppException(ErrorCode.AUTH_FORBIDDEN);
        }
    }
}
