package com.plateapp.plate_main.owner.service;

import com.plateapp.plate_main.auth.domain.User;
import com.plateapp.plate_main.auth.repository.UserRepository;
import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import com.plateapp.plate_main.common.s3.S3UploadService;
import com.plateapp.plate_main.owner.dto.OwnerStoreAnalyticsDtos;
import com.plateapp.plate_main.owner.repository.StoreOwnerRepository;
import com.plateapp.plate_main.restaurant.entity.Restaurant;
import com.plateapp.plate_main.restaurant.repository.RestaurantRepository;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OwnerStoreAnalyticsService {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final int MAX_TREND_DAYS = 93;

    private final NamedParameterJdbcTemplate jdbc;
    private final UserRepository userRepository;
    private final StoreOwnerRepository storeOwnerRepository;
    private final RestaurantRepository restaurantRepository;
    private final S3UploadService s3UploadService;

    @Transactional(readOnly = true)
    public OwnerStoreAnalyticsDtos.SummaryResponse summary(
            String username,
            Long storeId,
            LocalDate from,
            LocalDate to
    ) {
        AnalyticsContext context = context(username, storeId);
        DateRange current = range(from, to);
        DateRange previous = previousRange(current);

        long impressions = countHomeImpressions(context.videoStoreIds(), current);
        long previousImpressions = countHomeImpressions(context.videoStoreIds(), previous);
        long views = countViews(context.videoStoreIds(), current);
        long previousViews = countViews(context.videoStoreIds(), previous);
        long uniqueViewers = countUniqueViewers(context.videoStoreIds(), current);
        long previousUniqueViewers = countUniqueViewers(context.videoStoreIds(), previous);
        long completedViews = countCompletedViews(context.videoStoreIds(), current);
        long newSaves = countNewSaves(context.videoStoreIds(), current);
        long previousNewSaves = countNewSaves(context.videoStoreIds(), previous);
        long activeSaves = countActiveSaves(context.videoStoreIds());
        long comments = countComments(context.videoStoreIds(), current);
        long previousComments = countComments(context.videoStoreIds(), previous);
        double averageWatchSeconds = averageWatchSeconds(context.videoStoreIds(), current);
        double completionRate = ratio(completedViews, views);

        Map<String, Long> eventCounts = eventCounts(context.videoStoreIds(), current);
        long eventImpressions = eventCounts.getOrDefault("IMPRESSION", 0L);
        long clicks = eventCounts.getOrDefault("CLICK", 0L);
        long plays = eventCounts.getOrDefault("PLAY", 0L);
        long completes = eventCounts.getOrDefault("COMPLETE", 0L);
        long hides = eventCounts.getOrDefault("HIDE", 0L);
        long reports = eventCounts.getOrDefault("REPORT", 0L);

        return new OwnerStoreAnalyticsDtos.SummaryResponse(
                context.source(),
                current.from(),
                current.to(),
                List.of(
                        metric("homeImpressions", "Home impressions", impressions,
                                changeRate(impressions, previousImpressions), "count", "previous_period"),
                        metric("videoViews", "Video views", views,
                                changeRate(views, previousViews), "count", "previous_period"),
                        metric("uniqueViewers", "Unique viewers", uniqueViewers,
                                changeRate(uniqueViewers, previousUniqueViewers), "count", "previous_period"),
                        metric("activeSaves", "Active saves", activeSaves,
                                null, "count", "current"),
                        metric("newSaves", "New saves", newSaves,
                                changeRate(newSaves, previousNewSaves), "count", "previous_period"),
                        metric("comments", "Comments", comments,
                                changeRate(comments, previousComments), "count", "previous_period")
                ),
                new OwnerStoreAnalyticsDtos.WatchSummary(
                        views,
                        uniqueViewers,
                        completedViews,
                        round2(averageWatchSeconds),
                        round4(completionRate)
                ),
                new OwnerStoreAnalyticsDtos.EngagementSummary(
                        impressions,
                        activeSaves,
                        newSaves,
                        comments
                ),
                new OwnerStoreAnalyticsDtos.FunnelSummary(
                        eventImpressions,
                        clicks,
                        plays,
                        completes,
                        hides,
                        reports,
                        round4(ratio(clicks, eventImpressions)),
                        round4(ratio(plays, eventImpressions)),
                        round4(ratio(completes, plays))
                )
        );
    }

    @Transactional(readOnly = true)
    public OwnerStoreAnalyticsDtos.TrendResponse trends(
            String username,
            Long storeId,
            LocalDate from,
            LocalDate to,
            String interval
    ) {
        AnalyticsContext context = context(username, storeId);
        DateRange dateRange = range(from, to);
        if (interval != null && !"day".equalsIgnoreCase(interval)) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "interval must be day.");
        }
        long days = ChronoUnit.DAYS.between(dateRange.from(), dateRange.toExclusive());
        if (days > MAX_TREND_DAYS) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "trend range must be 93 days or less.");
        }

        Map<LocalDate, Long> impressions = dailyHomeImpressions(context.videoStoreIds(), dateRange);
        Map<LocalDate, Long> views = dailyViews(context.videoStoreIds(), dateRange, false);
        Map<LocalDate, Long> completedViews = dailyViews(context.videoStoreIds(), dateRange, true);
        Map<LocalDate, Long> saves = dailyNewSaves(context.videoStoreIds(), dateRange);
        Map<LocalDate, Long> comments = dailyComments(context.videoStoreIds(), dateRange);

        List<OwnerStoreAnalyticsDtos.TrendPoint> points = new ArrayList<>();
        for (LocalDate date = dateRange.from(); date.isBefore(dateRange.toExclusive()); date = date.plusDays(1)) {
            points.add(new OwnerStoreAnalyticsDtos.TrendPoint(
                    date,
                    impressions.getOrDefault(date, 0L),
                    views.getOrDefault(date, 0L),
                    completedViews.getOrDefault(date, 0L),
                    saves.getOrDefault(date, 0L),
                    comments.getOrDefault(date, 0L)
            ));
        }

        return new OwnerStoreAnalyticsDtos.TrendResponse(
                context.source(),
                dateRange.from(),
                dateRange.to(),
                "day",
                points
        );
    }

    @Transactional(readOnly = true)
    public OwnerStoreAnalyticsDtos.ContentPerformanceResponse contents(
            String username,
            Long storeId,
            LocalDate from,
            LocalDate to,
            int page,
            int size
    ) {
        AnalyticsContext context = context(username, storeId);
        DateRange dateRange = range(from, to);
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));
        if (context.videoStoreIds().isEmpty()) {
            return new OwnerStoreAnalyticsDtos.ContentPerformanceResponse(
                    context.source(),
                    dateRange.from(),
                    dateRange.to(),
                    List.of(),
                    safePage,
                    safeSize,
                    0,
                    0,
                    false
            );
        }

        long total = context.videoStoreIds().size();
        MapSqlParameterSource params = params(context.videoStoreIds(), dateRange)
                .addValue("limit", safeSize)
                .addValue("offset", safePage * safeSize);
        List<OwnerStoreAnalyticsDtos.ContentPerformanceItem> content = jdbc.query("""
                select
                    s.store_id,
                    s.title,
                    coalesce(nullif(s.store_name, ''), s.title) as store_name,
                    s.thumbnail,
                    s.created_at,
                    coalesce(i.impressions, 0) as impressions,
                    coalesce(w.views, 0) as views,
                    coalesce(w.unique_viewers, 0) as unique_viewers,
                    coalesce(w.completed_views, 0) as completed_views,
                    coalesce(w.average_watch_seconds, 0) as average_watch_seconds,
                    coalesce(l.active_save_count, 0) as active_save_count,
                    coalesce(ns.new_save_count, 0) as new_save_count,
                    coalesce(c.comment_count, 0) as comment_count
                from fp_300 s
                left join (
                    select store_id, count(*) as impressions
                    from fp_376
                    where content_type = 'VIDEO'
                      and store_id in (:storeIds)
                      and impressed_at >= :fromLocal and impressed_at < :toLocal
                    group by store_id
                ) i on i.store_id = s.store_id
                left join (
                    select store_id,
                           count(*) as views,
                           count(distinct username) as unique_viewers,
                           count(*) filter (where completion_status = true) as completed_views,
                           avg(duration_watched) as average_watch_seconds
                    from fp_305 w
                    where w.store_id in (:storeIds)
                      and w.use_yn = 'Y'
                      and w.deleted_at is null
                      and w.timestamp >= :fromInstant and w.timestamp < :toInstant
                    group by store_id
                ) w on w.store_id = s.store_id
                left join (
                    select store_id, count(*) as active_save_count
                    from fp_50
                    where store_id in (:storeIds)
                      and use_yn = 'Y'
                      and deleted_at is null
                    group by store_id
                ) l on l.store_id = s.store_id
                left join (
                    select store_id, count(*) as new_save_count
                    from fp_50
                    where store_id in (:storeIds)
                      and use_yn = 'Y'
                      and deleted_at is null
                      and created_at >= :fromLocal and created_at < :toLocal
                    group by store_id
                ) ns on ns.store_id = s.store_id
                left join (
                    select store_id, count(*) as comment_count
                    from fp_440
                    where store_id in (:storeIds)
                      and use_yn = 'Y'
                      and deleted_at is null
                      and created_at >= :fromLocal and created_at < :toLocal
                    group by store_id
                ) c on c.store_id = s.store_id
                where s.store_id in (:storeIds)
                order by coalesce(w.views, 0) desc,
                         coalesce(i.impressions, 0) desc,
                         s.created_at desc nulls last,
                         s.store_id desc
                limit :limit offset :offset
                """, params, contentMapper());

        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / safeSize);
        return new OwnerStoreAnalyticsDtos.ContentPerformanceResponse(
                context.source(),
                dateRange.from(),
                dateRange.to(),
                content,
                safePage,
                safeSize,
                total,
                totalPages,
                safePage + 1 < totalPages
        );
    }

    private AnalyticsContext context(String username, Long storeId) {
        if (storeId == null) {
            throw new AppException(ErrorCode.COMMON_MISSING_PARAMETER, "storeId is required.");
        }
        Integer userId = currentUserId(username);
        if (!storeOwnerRepository.existsByStoreIdAndUserIdAndRevokedAtIsNull(storeId, userId)) {
            throw new AppException(ErrorCode.COMMON_NOT_FOUND);
        }

        Restaurant restaurant = restaurantRepository.findById(storeId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMON_NOT_FOUND));
        List<String> ownerUsernames = activeOwnerUsernames(storeId, username);
        List<Integer> videoStoreIds = linkedVideoStoreIds(storeId, restaurant, ownerUsernames);

        OwnerStoreAnalyticsDtos.AnalyticsSource source = new OwnerStoreAnalyticsDtos.AnalyticsSource(
                storeId,
                restaurant.getTitle(),
                restaurant.getAddress(),
                videoStoreIds,
                "restaurant_id_or_owner_name_address",
                !videoStoreIds.isEmpty()
        );
        return new AnalyticsContext(source, videoStoreIds);
    }

    private List<String> activeOwnerUsernames(Long storeId, String fallbackUsername) {
        List<String> usernames = jdbc.queryForList("""
                select u.username
                from store_owners so
                join fp_100 u on u.user_id = so.user_id
                where so.store_id = :storeId
                  and so.revoked_at is null
                  and u.username is not null
                order by so.created_at desc, so.id desc
                """, new MapSqlParameterSource("storeId", storeId), String.class);
        if (usernames.isEmpty() && fallbackUsername != null && !fallbackUsername.isBlank()) {
            return List.of(fallbackUsername);
        }
        return usernames;
    }

    private List<Integer> linkedVideoStoreIds(Long storeId, Restaurant restaurant, List<String> ownerUsernames) {
        if (ownerUsernames.isEmpty()) {
            return List.of();
        }
        Integer storeIdInt = storeId <= Integer.MAX_VALUE ? storeId.intValue() : null;
        List<Integer> ids = jdbc.queryForList("""
                select distinct s.store_id
                from fp_300 s
                where s.username in (:ownerUsernames)
                  and s.use_yn = 'Y'
                  and s.deleted_at is null
                  and (
                        (:storeIdInt is not null and s.store_id = :storeIdInt)
                     or (
                        lower(trim(coalesce(nullif(s.store_name, ''), s.title, ''))) = lower(trim(:storeName))
                        and lower(trim(coalesce(s.address, ''))) = lower(trim(:address))
                     )
                  )
                order by s.store_id desc
                """, new MapSqlParameterSource()
                .addValue("ownerUsernames", ownerUsernames)
                .addValue("storeIdInt", storeIdInt)
                .addValue("storeName", restaurant.getTitle())
                .addValue("address", restaurant.getAddress()), Integer.class);
        return List.copyOf(new LinkedHashSet<>(ids));
    }

    private int currentUserId(String username) {
        if (username == null || username.isBlank()) {
            throw new AppException(ErrorCode.AUTH_UNAUTHORIZED);
        }
        Integer userId = userRepository.findUserIdByUsername(username);
        if (userId == null) {
            User user = userRepository.findById(username)
                    .orElseThrow(() -> new AppException(ErrorCode.AUTH_UNAUTHORIZED));
            userId = user.getUserId();
        }
        if (userId == null) {
            throw new AppException(ErrorCode.COMMON_INTERNAL_ERROR, "User id is missing.");
        }
        return userId;
    }

    private DateRange range(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new AppException(ErrorCode.COMMON_MISSING_PARAMETER, "from and to are required.");
        }
        if (from.isAfter(to)) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "from must be on or before to.");
        }
        return new DateRange(from, to, to.plusDays(1));
    }

    private DateRange previousRange(DateRange current) {
        long days = ChronoUnit.DAYS.between(current.from(), current.toExclusive());
        LocalDate previousTo = current.from().minusDays(1);
        LocalDate previousFrom = current.from().minusDays(days);
        return new DateRange(previousFrom, previousTo, current.from());
    }

    private MapSqlParameterSource params(List<Integer> storeIds, DateRange range) {
        return new MapSqlParameterSource()
                .addValue("storeIds", storeIds)
                .addValue("fromLocal", range.from().atStartOfDay())
                .addValue("toLocal", range.toExclusive().atStartOfDay())
                .addValue("fromInstant", range.from().atStartOfDay(SEOUL).toOffsetDateTime())
                .addValue("toInstant", range.toExclusive().atStartOfDay(SEOUL).toOffsetDateTime());
    }

    private long countHomeImpressions(List<Integer> storeIds, DateRange range) {
        return countWhenLinked(storeIds, """
                select count(*)
                from fp_376
                where content_type = 'VIDEO'
                  and store_id in (:storeIds)
                  and impressed_at >= :fromLocal and impressed_at < :toLocal
                """, range);
    }

    private long countViews(List<Integer> storeIds, DateRange range) {
        return countWhenLinked(storeIds, """
                select count(*)
                from fp_305 w
                where w.store_id in (:storeIds)
                  and w.use_yn = 'Y'
                  and w.deleted_at is null
                  and w.timestamp >= :fromInstant and w.timestamp < :toInstant
                """, range);
    }

    private long countUniqueViewers(List<Integer> storeIds, DateRange range) {
        return countWhenLinked(storeIds, """
                select count(distinct username)
                from fp_305 w
                where w.store_id in (:storeIds)
                  and w.use_yn = 'Y'
                  and w.deleted_at is null
                  and w.timestamp >= :fromInstant and w.timestamp < :toInstant
                """, range);
    }

    private long countCompletedViews(List<Integer> storeIds, DateRange range) {
        return countWhenLinked(storeIds, """
                select count(*)
                from fp_305 w
                where w.store_id in (:storeIds)
                  and w.use_yn = 'Y'
                  and w.deleted_at is null
                  and w.completion_status = true
                  and w.timestamp >= :fromInstant and w.timestamp < :toInstant
                """, range);
    }

    private long countActiveSaves(List<Integer> storeIds) {
        if (storeIds.isEmpty()) {
            return 0;
        }
        return number(jdbc.queryForObject("""
                select count(*)
                from fp_50
                where store_id in (:storeIds)
                  and use_yn = 'Y'
                  and deleted_at is null
                """, new MapSqlParameterSource("storeIds", storeIds), Number.class));
    }

    private long countNewSaves(List<Integer> storeIds, DateRange range) {
        return countWhenLinked(storeIds, """
                select count(*)
                from fp_50
                where store_id in (:storeIds)
                  and use_yn = 'Y'
                  and deleted_at is null
                  and created_at >= :fromLocal and created_at < :toLocal
                """, range);
    }

    private long countComments(List<Integer> storeIds, DateRange range) {
        return countWhenLinked(storeIds, """
                select count(*)
                from fp_440
                where store_id in (:storeIds)
                  and use_yn = 'Y'
                  and deleted_at is null
                  and created_at >= :fromLocal and created_at < :toLocal
                """, range);
    }

    private double averageWatchSeconds(List<Integer> storeIds, DateRange range) {
        if (storeIds.isEmpty()) {
            return 0.0;
        }
        Number value = jdbc.queryForObject("""
                select coalesce(avg(duration_watched), 0)
                from fp_305 w
                where w.store_id in (:storeIds)
                  and w.use_yn = 'Y'
                  and w.deleted_at is null
                  and w.duration_watched is not null
                  and w.timestamp >= :fromInstant and w.timestamp < :toInstant
                """, params(storeIds, range), Number.class);
        return value == null ? 0.0 : value.doubleValue();
    }

    private Map<String, Long> eventCounts(List<Integer> storeIds, DateRange range) {
        if (storeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Long> result = new LinkedHashMap<>();
        jdbc.query("""
                select upper(event_type) as event_type, count(*) as cnt
                from fp_370
                where store_id in (:storeIds)
                  and server_event_at >= :fromLocal and server_event_at < :toLocal
                group by upper(event_type)
                """, params(storeIds, range), rs -> {
            result.put(rs.getString("event_type"), rs.getLong("cnt"));
        });
        return result;
    }

    private Map<LocalDate, Long> dailyHomeImpressions(List<Integer> storeIds, DateRange range) {
        return dailyCounts(storeIds, range, """
                select cast(impressed_at as date) as bucket, count(*) as cnt
                from fp_376
                where content_type = 'VIDEO'
                  and store_id in (:storeIds)
                  and impressed_at >= :fromLocal and impressed_at < :toLocal
                group by cast(impressed_at as date)
                """);
    }

    private Map<LocalDate, Long> dailyViews(List<Integer> storeIds, DateRange range, boolean completedOnly) {
        String completedClause = completedOnly ? " and w.completion_status = true\n" : "";
        return dailyCounts(storeIds, range, """
                select cast(w.timestamp as date) as bucket, count(*) as cnt
                from fp_305 w
                where w.store_id in (:storeIds)
                  and w.use_yn = 'Y'
                  and w.deleted_at is null
                  and w.timestamp >= :fromInstant and w.timestamp < :toInstant
                """ + completedClause + """
                group by cast(w.timestamp as date)
                """);
    }

    private Map<LocalDate, Long> dailyNewSaves(List<Integer> storeIds, DateRange range) {
        return dailyCounts(storeIds, range, """
                select cast(created_at as date) as bucket, count(*) as cnt
                from fp_50
                where store_id in (:storeIds)
                  and use_yn = 'Y'
                  and deleted_at is null
                  and created_at >= :fromLocal and created_at < :toLocal
                group by cast(created_at as date)
                """);
    }

    private Map<LocalDate, Long> dailyComments(List<Integer> storeIds, DateRange range) {
        return dailyCounts(storeIds, range, """
                select cast(created_at as date) as bucket, count(*) as cnt
                from fp_440
                where store_id in (:storeIds)
                  and use_yn = 'Y'
                  and deleted_at is null
                  and created_at >= :fromLocal and created_at < :toLocal
                group by cast(created_at as date)
                """);
    }

    private Map<LocalDate, Long> dailyCounts(List<Integer> storeIds, DateRange range, String sql) {
        if (storeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<LocalDate, Long> result = new LinkedHashMap<>();
        jdbc.query(sql, params(storeIds, range), rs -> {
            result.put(toLocalDate(rs.getObject("bucket")), rs.getLong("cnt"));
        });
        return result;
    }

    private long countWhenLinked(List<Integer> storeIds, String sql, DateRange range) {
        if (storeIds.isEmpty()) {
            return 0;
        }
        return number(jdbc.queryForObject(sql, params(storeIds, range), Number.class));
    }

    private RowMapper<OwnerStoreAnalyticsDtos.ContentPerformanceItem> contentMapper() {
        return (rs, rowNum) -> {
            long views = rs.getLong("views");
            long completedViews = rs.getLong("completed_views");
            return new OwnerStoreAnalyticsDtos.ContentPerformanceItem(
                    rs.getInt("store_id"),
                    rs.getString("title"),
                    rs.getString("store_name"),
                    s3UploadService.toImageUrl(rs.getString("thumbnail")),
                    toLocalDate(rs.getObject("created_at")),
                    rs.getLong("impressions"),
                    views,
                    rs.getLong("unique_viewers"),
                    completedViews,
                    round2(rs.getDouble("average_watch_seconds")),
                    round4(ratio(completedViews, views)),
                    rs.getLong("active_save_count"),
                    rs.getLong("new_save_count"),
                    rs.getLong("comment_count")
            );
        };
    }

    private OwnerStoreAnalyticsDtos.Metric metric(
            String key,
            String label,
            long value,
            Double changeRate,
            String unit,
            String comparison
    ) {
        return new OwnerStoreAnalyticsDtos.Metric(key, label, value, changeRate, unit, comparison);
    }

    private Double changeRate(long current, long previous) {
        if (previous == 0) {
            return current == 0 ? 0.0 : null;
        }
        return round2((current - previous) * 100.0 / previous);
    }

    private long number(Number value) {
        return value == null ? 0 : value.longValue();
    }

    private double ratio(long numerator, long denominator) {
        return denominator <= 0 ? 0.0 : (double) numerator / denominator;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private double round4(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    private LocalDate toLocalDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof Date date) {
            return date.toLocalDate();
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime().toLocalDate();
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.toLocalDate();
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.atZoneSameInstant(SEOUL).toLocalDate();
        }
        return LocalDate.parse(String.valueOf(value).substring(0, 10));
    }

    private record AnalyticsContext(
            OwnerStoreAnalyticsDtos.AnalyticsSource source,
            List<Integer> videoStoreIds
    ) {
    }

    private record DateRange(LocalDate from, LocalDate to, LocalDate toExclusive) {
    }
}
