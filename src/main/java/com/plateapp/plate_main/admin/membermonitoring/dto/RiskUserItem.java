package com.plateapp.plate_main.admin.membermonitoring.dto;

public record RiskUserItem(
    String username,
    long reportCount,
    long blockedCount,
    String recentActivityLabel,
    String recommendedAction,
    int score
) {
}
