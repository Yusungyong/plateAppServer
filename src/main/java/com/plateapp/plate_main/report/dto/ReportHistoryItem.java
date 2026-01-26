package com.plateapp.plate_main.report.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ReportHistoryItem {
    Integer reportId;
    String targetType;
    Integer targetId;
    String targetUsername;
    String reason;
    String description;
    String placeId;
    String storeName;
    String thumbnail;
    String status;
    LocalDateTime createdAt;
    LocalDateTime resolvedAt;
}
