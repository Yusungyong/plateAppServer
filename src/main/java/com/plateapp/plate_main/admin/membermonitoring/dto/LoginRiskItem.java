package com.plateapp.plate_main.admin.membermonitoring.dto;

import java.time.LocalDateTime;

public record LoginRiskItem(
    String username,
    String riskType,
    String riskLabel,
    String detail,
    String ipAddress,
    String deviceId,
    int score,
    LocalDateTime lastOccurredAt
) {
}
