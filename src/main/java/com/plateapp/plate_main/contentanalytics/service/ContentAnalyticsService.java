package com.plateapp.plate_main.contentanalytics.service;

import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import com.plateapp.plate_main.common.s3.S3UploadService;
import com.plateapp.plate_main.contentanalytics.dto.ContentAnalyticsDtos;
import com.plateapp.plate_main.contentanalytics.repository.ContentAnalyticsQueryRepository;
import com.plateapp.plate_main.contentanalytics.repository.ContentAnalyticsQueryRepository.ContentCounts;
import com.plateapp.plate_main.contentanalytics.repository.ContentAnalyticsQueryRepository.ContentMetricRow;
import com.plateapp.plate_main.contentanalytics.repository.ContentAnalyticsQueryRepository.DailyMetric;
import com.plateapp.plate_main.contentanalytics.repository.ContentAnalyticsQueryRepository.DateRange;
import com.plateapp.plate_main.contentanalytics.repository.ContentAnalyticsQueryRepository.SummaryMetrics;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContentAnalyticsService {

    private static final String TIMEZONE = "Asia/Seoul";
    private static final int MAX_TREND_DAYS = 93;
    private static final Set<String> CONTENT_TYPES = Set.of("all", "video", "image");
    private static final Set<String> CONTENT_SORTS = Set.of(
            "impressions", "views", "likes", "comments", "recent"
    );

    private final ContentAnalyticsQueryRepository repository;
    private final S3UploadService s3UploadService;

    @Transactional(readOnly = true)
    public ContentAnalyticsDtos.SummaryResponse summary(String username, LocalDate from, LocalDate to) {
        requireUsername(username);
        DateRange range = range(from, to);
        ContentCounts counts = repository.loadContentCounts(username);
        SummaryMetrics metrics = repository.loadSummaryMetrics(username, range);

        long periodEngagements = metrics.periodLikeCount()
                + metrics.commentCount()
                + metrics.replyCount()
                + metrics.shareCount();

        return new ContentAnalyticsDtos.SummaryResponse(
                period(range),
                new ContentAnalyticsDtos.ContentSummary(
                        counts.totalCount(),
                        counts.videoCount(),
                        counts.imageCount(),
                        counts.publicCount(),
                        counts.privateCount(),
                        counts.unknownVisibilityCount()
                ),
                new ContentAnalyticsDtos.ExposureSummary(
                        metrics.impressionCount(),
                        metrics.uniqueAudienceCount()
                ),
                new ContentAnalyticsDtos.EngagementSummary(
                        metrics.activeLikeCount(),
                        metrics.periodLikeCount(),
                        metrics.commentCount(),
                        metrics.replyCount(),
                        metrics.shareCount(),
                        ratio(periodEngagements, metrics.impressionCount())
                ),
                new ContentAnalyticsDtos.VideoSummary(
                        metrics.watchSessionCount(),
                        metrics.uniqueViewerCount(),
                        metrics.watchSeconds(),
                        round2(metrics.averageWatchSeconds()),
                        metrics.completedViewCount(),
                        ratio(metrics.completedViewCount(), metrics.watchSessionCount())
                )
        );
    }

    @Transactional(readOnly = true)
    public ContentAnalyticsDtos.TrendsResponse trends(
            String username,
            LocalDate from,
            LocalDate to,
            String interval
    ) {
        requireUsername(username);
        DateRange range = range(from, to);
        if (!"day".equals(normalize(interval))) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "interval must be day.");
        }
        long days = ChronoUnit.DAYS.between(range.from(), range.to()) + 1;
        if (days > MAX_TREND_DAYS) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "trend range must be 93 days or less.");
        }

        Map<LocalDate, TrendAccumulator> values = new LinkedHashMap<>();
        for (DailyMetric metric : repository.loadDailyMetrics(username, range)) {
            if (metric.date() == null || metric.date().isBefore(range.from()) || metric.date().isAfter(range.to())) {
                continue;
            }
            values.computeIfAbsent(metric.date(), ignored -> new TrendAccumulator())
                    .add(metric.metric(), metric.value());
        }

        List<ContentAnalyticsDtos.TrendPoint> points = new ArrayList<>((int) days);
        for (LocalDate date = range.from(); !date.isAfter(range.to()); date = date.plusDays(1)) {
            TrendAccumulator value = values.getOrDefault(date, new TrendAccumulator());
            points.add(new ContentAnalyticsDtos.TrendPoint(
                    date,
                    value.impressions,
                    value.uniqueAudience,
                    value.videoPlayStarts,
                    value.videoCompletes,
                    value.watchSeconds,
                    value.newActiveLikes,
                    value.comments,
                    value.replies,
                    value.shares
            ));
        }

        return new ContentAnalyticsDtos.TrendsResponse(period(range), "day", points);
    }

    @Transactional(readOnly = true)
    public ContentAnalyticsDtos.ContentPageResponse contents(
            String username,
            LocalDate from,
            LocalDate to,
            String type,
            String sort,
            int page,
            int size
    ) {
        requireUsername(username);
        DateRange range = range(from, to);
        String normalizedType = normalize(type);
        String normalizedSort = normalize(sort);
        if (!CONTENT_TYPES.contains(normalizedType)) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "type must be all, video, or image.");
        }
        if (!CONTENT_SORTS.contains(normalizedSort)) {
            throw new AppException(
                    ErrorCode.COMMON_INVALID_INPUT,
                    "sort must be impressions, views, likes, comments, or recent."
            );
        }
        if (page < 0) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "page must be 0 or greater.");
        }
        if (size < 1 || size > 100) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "size must be between 1 and 100.");
        }

        long totalElements = repository.countContents(username, normalizedType);
        List<ContentAnalyticsDtos.ContentItem> items = repository.findContents(
                        username,
                        range,
                        normalizedType,
                        normalizedSort,
                        page,
                        size
                ).stream()
                .map(this::contentItem)
                .toList();

        long calculatedPages = totalElements == 0 ? 0 : (totalElements + size - 1) / size;
        int totalPages = calculatedPages > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) calculatedPages;
        boolean hasNext = ((long) page + 1) * size < totalElements;

        return new ContentAnalyticsDtos.ContentPageResponse(
                items,
                page,
                size,
                totalElements,
                totalPages,
                hasNext
        );
    }

    private ContentAnalyticsDtos.ContentItem contentItem(ContentMetricRow row) {
        boolean video = "VIDEO".equals(row.contentType());
        long shareCount = value(row.shareCount());
        long periodEngagements = row.periodLikeCount() + row.commentCount() + row.replyCount() + shareCount;

        ContentAnalyticsDtos.ContentMetrics metrics = new ContentAnalyticsDtos.ContentMetrics(
                row.impressionCount(),
                row.uniqueAudienceCount(),
                row.activeLikeCount(),
                row.periodLikeCount(),
                row.commentCount(),
                row.replyCount(),
                ratio(periodEngagements, row.impressionCount())
        );

        ContentAnalyticsDtos.VideoMetrics videoMetrics = video
                ? new ContentAnalyticsDtos.VideoMetrics(
                        value(row.watchSessionCount()),
                        value(row.uniqueViewerCount()),
                        value(row.watchSeconds()),
                        round2(row.averageWatchSeconds()),
                        value(row.completedViewCount()),
                        ratio(value(row.completedViewCount()), value(row.watchSessionCount())),
                        shareCount
                )
                : null;

        String thumbnailPath = firstText(row.thumbnail(), firstImage(row.images()));
        String thumbnailUrl = video
                ? s3UploadService.toImageUrl(thumbnailPath)
                : s3UploadService.toFeedImageUrl(thumbnailPath);

        return new ContentAnalyticsDtos.ContentItem(
                row.contentType(),
                (video ? "video:" : "image:") + row.numericId(),
                video ? row.numericId() : null,
                video ? null : row.numericId(),
                row.title(),
                thumbnailUrl,
                row.publishedOn(),
                row.visibility(),
                video ? null : imageCount(row.images()),
                metrics,
                videoMetrics
        );
    }

    private DateRange range(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new AppException(ErrorCode.COMMON_MISSING_PARAMETER, "from and to are required.");
        }
        if (from.isAfter(to)) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "from must be on or before to.");
        }
        try {
            return DateRange.of(from, to);
        } catch (DateTimeException exception) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "date range is invalid.");
        }
    }

    private ContentAnalyticsDtos.Period period(DateRange range) {
        return new ContentAnalyticsDtos.Period(range.from(), range.to(), TIMEZONE);
    }

    private void requireUsername(String username) {
        if (username == null || username.isBlank() || "anonymousUser".equalsIgnoreCase(username)) {
            throw new AppException(ErrorCode.AUTH_UNAUTHORIZED);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private Double ratio(long numerator, long denominator) {
        if (denominator == 0) {
            return null;
        }
        return round4((double) numerator / denominator);
    }

    private Double round2(Double value) {
        if (value == null) {
            return null;
        }
        return Math.round(value * 100.0) / 100.0;
    }

    private Double round4(double value) {
        return Math.round(value * 10_000.0) / 10_000.0;
    }

    private long value(Long value) {
        return value == null ? 0L : value;
    }

    private String firstText(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first.trim();
        }
        return second == null || second.isBlank() ? null : second.trim();
    }

    private String firstImage(String images) {
        if (images == null || images.isBlank()) {
            return null;
        }
        for (String image : images.split(",")) {
            if (!image.isBlank()) {
                return image.trim();
            }
        }
        return null;
    }

    private int imageCount(String images) {
        if (images == null || images.isBlank()) {
            return 0;
        }
        int count = 0;
        for (String image : images.split(",")) {
            if (!image.isBlank()) {
                count++;
            }
        }
        return count;
    }

    private static final class TrendAccumulator {
        private long impressions;
        private long uniqueAudience;
        private long videoPlayStarts;
        private long videoCompletes;
        private long watchSeconds;
        private long newActiveLikes;
        private long comments;
        private long replies;
        private long shares;

        private TrendAccumulator add(String metric, long value) {
            switch (metric) {
                case "IMPRESSION" -> impressions += value;
                case "UNIQUE_AUDIENCE" -> uniqueAudience += value;
                case "VIDEO_PLAY_START" -> videoPlayStarts += value;
                case "VIDEO_COMPLETE" -> videoCompletes += value;
                case "WATCH_SECONDS" -> watchSeconds += value;
                case "NEW_ACTIVE_LIKE" -> newActiveLikes += value;
                case "COMMENT" -> comments += value;
                case "REPLY" -> replies += value;
                case "SHARE" -> shares += value;
                default -> {
                    // Ignore unknown metrics so adding a repository-side metric stays backwards compatible.
                }
            }
            return this;
        }
    }
}
