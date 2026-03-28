package com.plateapp.plate_main.admin.membermonitoring.dto;

public record MemberMonitoringSummaryResponse(
    long totalUsers,
    long newUsersToday,
    long activeUsers7d,
    double loginFailureRateToday,
    long pendingRoleChanges,
    long riskUsers24h
) {
}
