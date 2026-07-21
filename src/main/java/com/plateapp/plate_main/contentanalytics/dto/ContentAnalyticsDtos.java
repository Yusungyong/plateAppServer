package com.plateapp.plate_main.contentanalytics.dto;

import java.time.LocalDate;
import java.util.List;

public final class ContentAnalyticsDtos {

    private ContentAnalyticsDtos() {
    }

    public record Period(
            LocalDate from,
            LocalDate to,
            String timezone
    ) {
    }

    public record ContentSummary(
            long totalCount,
            long videoCount,
            long imageCount,
            long publicCount,
            long privateCount,
            long unknownVisibilityCount
    ) {
    }

    public record ExposureSummary(
            long impressionCount,
            long uniqueAudienceCount
    ) {
    }

    public record EngagementSummary(
            long activeReceivedLikeCount,
            long periodNewActiveLikeCount,
            long periodCommentCount,
            long periodReplyCount,
            long periodShareCount,
            Double engagementRate
    ) {
    }

    public record VideoSummary(
            long watchSessionCount,
            long uniqueViewerCount,
            long totalWatchSeconds,
            Double averageWatchSeconds,
            long completedViewCount,
            Double completionRate
    ) {
    }

    public record SummaryResponse(
            Period period,
            ContentSummary content,
            ExposureSummary exposure,
            EngagementSummary engagement,
            VideoSummary video
    ) {
    }

    public record TrendPoint(
            LocalDate date,
            long impressionCount,
            long uniqueAudienceCount,
            long videoPlayStartCount,
            long videoCompleteCount,
            long watchSeconds,
            long newActiveLikeCount,
            long commentCount,
            long replyCount,
            long shareCount
    ) {
    }

    public record TrendsResponse(
            Period period,
            String interval,
            List<TrendPoint> points
    ) {
    }

    public record ContentMetrics(
            long impressionCount,
            long uniqueAudienceCount,
            long activeLikeCount,
            long periodNewActiveLikeCount,
            long periodCommentCount,
            long periodReplyCount,
            Double engagementRate
    ) {
    }

    public record VideoMetrics(
            long watchSessionCount,
            long uniqueViewerCount,
            long totalWatchSeconds,
            Double averageWatchSeconds,
            long completedViewCount,
            Double completionRate,
            long shareCount
    ) {
    }

    public record ContentItem(
            String contentType,
            String contentId,
            Integer videoStoreId,
            Integer imageFeedId,
            String title,
            String thumbnailUrl,
            LocalDate publishedOn,
            String visibility,
            Integer imageCount,
            ContentMetrics metrics,
            VideoMetrics videoMetrics
    ) {
    }

    public record ContentPageResponse(
            List<ContentItem> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean hasNext
    ) {
    }
}
