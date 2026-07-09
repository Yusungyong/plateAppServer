package com.plateapp.plate_main.owner.dto;

import java.time.LocalDate;
import java.util.List;

public final class OwnerStoreAnalyticsDtos {

    private OwnerStoreAnalyticsDtos() {
    }

    public record AnalyticsSource(
            Long storeId,
            String storeName,
            String address,
            List<Integer> videoStoreIds,
            String matchStrategy,
            boolean hasLinkedVideoContent
    ) {
    }

    public record Metric(
            String key,
            String label,
            long value,
            Double changeRate,
            String unit,
            String comparison
    ) {
    }

    public record WatchSummary(
            long totalViews,
            long uniqueViewers,
            long completedViews,
            double averageWatchSeconds,
            double completionRate
    ) {
    }

    public record EngagementSummary(
            long impressions,
            long activeSaveCount,
            long newSaveCount,
            long commentCount
    ) {
    }

    public record FunnelSummary(
            long impressions,
            long clicks,
            long plays,
            long completes,
            long hides,
            long reports,
            double clickThroughRate,
            double playRate,
            double completeRate
    ) {
    }

    public record SummaryResponse(
            AnalyticsSource source,
            LocalDate from,
            LocalDate to,
            List<Metric> metrics,
            WatchSummary watch,
            EngagementSummary engagement,
            FunnelSummary funnel
    ) {
    }

    public record TrendPoint(
            LocalDate date,
            long impressions,
            long views,
            long completedViews,
            long saves,
            long comments
    ) {
    }

    public record TrendResponse(
            AnalyticsSource source,
            LocalDate from,
            LocalDate to,
            String interval,
            List<TrendPoint> points
    ) {
    }

    public record ContentPerformanceItem(
            Integer videoStoreId,
            String title,
            String storeName,
            String thumbnailUrl,
            LocalDate createdAt,
            long impressions,
            long views,
            long uniqueViewers,
            long completedViews,
            double averageWatchSeconds,
            double completionRate,
            long activeSaveCount,
            long newSaveCount,
            long commentCount
    ) {
    }

    public record ContentPerformanceResponse(
            AnalyticsSource source,
            LocalDate from,
            LocalDate to,
            List<ContentPerformanceItem> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean hasNext
    ) {
    }
}
