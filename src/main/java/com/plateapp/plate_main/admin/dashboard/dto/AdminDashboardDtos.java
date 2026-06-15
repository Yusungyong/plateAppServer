package com.plateapp.plate_main.admin.dashboard.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public final class AdminDashboardDtos {

    private AdminDashboardDtos() {
    }

    public record Metric(
            String key,
            String label,
            long value,
            Double changeRate,
            String comparison
    ) {
    }

    public record SummaryResponse(List<Metric> metrics) {
    }

    public record ActivityTrend(
            LocalDate date,
            long activeStoreCount,
            long postCount,
            long reactionCount
    ) {
    }

    public record RegionDistribution(
            String regionCode,
            String regionName,
            long postCount
    ) {
    }

    public record ActivityItem(
            Long id,
            OffsetDateTime occurredAt,
            String resourceType,
            String resourceId,
            String storeName,
            String action,
            String actionLabel,
            Integer operatorId,
            String operatorName,
            String status
    ) {
    }

    public record ActivityListResponse(
            List<ActivityItem> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean hasNext
    ) {
    }
}
