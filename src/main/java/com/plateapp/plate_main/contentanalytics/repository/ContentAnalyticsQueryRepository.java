package com.plateapp.plate_main.contentanalytics.repository;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ContentAnalyticsQueryRepository {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private static final String ACTIVE_CONTENT_CTES = """
            with active_videos as (
                select store_id, username, title, store_name, thumbnail, created_at, open_yn
                from fp_300
                where username = :username
                  and use_yn = 'Y'
                  and deleted_at is null
            ),
            active_images as (
                select feed_no, username, feed_title, store_name, thumbnail, images, created_at, open_yn
                from fp_400
                where username = :username
                  and use_yn = 'Y'
            )
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public ContentCounts loadContentCounts(String username) {
        String sql = ACTIVE_CONTENT_CTES + """
                , owned as (
                    select 'VIDEO' as content_type, open_yn from active_videos
                    union all
                    select 'IMAGE' as content_type, open_yn from active_images
                )
                select
                    count(*) as total_count,
                    count(*) filter (where content_type = 'VIDEO') as video_count,
                    count(*) filter (where content_type = 'IMAGE') as image_count,
                    count(*) filter (where upper(trim(open_yn)) = 'Y') as public_count,
                    count(*) filter (where upper(trim(open_yn)) = 'N') as private_count,
                    count(*) filter (
                        where open_yn is null or upper(trim(open_yn)) not in ('Y', 'N')
                    ) as unknown_count
                from owned
                """;

        return jdbc.queryForObject(sql, new MapSqlParameterSource("username", username), (rs, rowNum) ->
                new ContentCounts(
                        rs.getLong("total_count"),
                        rs.getLong("video_count"),
                        rs.getLong("image_count"),
                        rs.getLong("public_count"),
                        rs.getLong("private_count"),
                        rs.getLong("unknown_count")
                ));
    }

    public SummaryMetrics loadSummaryMetrics(String username, DateRange range) {
        String sql = ACTIVE_CONTENT_CTES + """
                , owned_content as (
                    select 'VIDEO' as content_type, store_id as content_id from active_videos
                    union all
                    select 'IMAGE' as content_type, feed_no as content_id from active_images
                ),
                impression_rows as (
                    select i.*,
                           case
                               when nullif(trim(i.username), '') is not null
                                   then 'u:' || trim(i.username)
                               when nullif(trim(i.guest_id), '') is not null
                                   then 'g:' || trim(i.guest_id)
                               when nullif(trim(i.device_id), '') is not null
                                   then 'd:' || trim(i.device_id)
                               when nullif(trim(i.session_id), '') is not null
                                   then 's:' || trim(i.session_id)
                               else null
                           end as actor_key
                    from fp_376 i
                    join owned_content o
                      on (o.content_type = 'VIDEO' and i.content_type = 'VIDEO' and i.store_id = o.content_id)
                      or (o.content_type = 'IMAGE' and i.content_type = 'IMAGE' and i.feed_no = o.content_id)
                    where i.impressed_at >= :fromLocal and i.impressed_at < :toLocal
                ),
                active_like_rows as (
                    select coalesce(l.updated_at, l.created_at) as activated_at
                    from fp_50 l
                    join active_videos v on v.store_id = l.store_id
                    where l.use_yn = 'Y' and l.deleted_at is null
                      and l.username <> :username
                    union all
                    select l.created_at as activated_at
                    from fp_60 l
                    join active_images i on i.feed_no = l.feed_id
                    where l.use_yn = 'Y' and l.deleted_at is null
                      and l.username <> :username
                ),
                comment_rows as (
                    select c.created_at
                    from fp_440 c
                    join active_videos v on v.store_id = c.store_id
                    where c.use_yn = 'Y' and c.deleted_at is null
                      and c.username <> :username
                      and c.created_at >= :fromLocal and c.created_at < :toLocal
                    union all
                    select c.created_at
                    from fp_460 c
                    join active_images i on i.feed_no = c.feed_id
                    where c.use_yn = 'Y' and c.deleted_at is null
                      and c.username <> :username
                      and c.created_at >= :fromLocal and c.created_at < :toLocal
                ),
                reply_rows as (
                    select r.created_at
                    from fp_450 r
                    join fp_440 c on c.comment_id = r.comment_id
                    join active_videos v on v.store_id = c.store_id
                    where r.use_yn = 'Y' and r.deleted_at is null
                      and c.use_yn = 'Y' and c.deleted_at is null
                      and r.username <> :username
                      and r.created_at >= :fromLocal and r.created_at < :toLocal
                    union all
                    select r.created_at
                    from fp_470 r
                    join fp_460 c on c.comment_id = r.comment_id
                    join active_images i on i.feed_no = c.feed_id
                    where r.use_yn = 'Y' and r.deleted_at is null
                      and c.use_yn = 'Y' and c.deleted_at is null
                      and r.username <> :username
                      and r.created_at >= :fromLocal and r.created_at < :toLocal
                ),
                watch_rows as (
                    select w.*
                    from fp_305 w
                    join active_videos v on v.store_id = w.store_id
                    where w.use_yn = 'Y' and w.deleted_at is null
                      and w.timestamp >= :fromInstant and w.timestamp < :toInstant
                ),
                share_rows as (
                    select e.event_id
                    from fp_370 e
                    join active_videos v on v.store_id = e.store_id
                    where upper(e.event_type) = 'SHARE'
                      and (e.username is null or e.username <> :username)
                      and e.server_event_at >= :fromLocal and e.server_event_at < :toLocal
                )
                select
                    (select count(*) from impression_rows) as impression_count,
                    (select count(distinct actor_key) from impression_rows where actor_key is not null)
                        as unique_audience_count,
                    (select count(*) from active_like_rows) as active_like_count,
                    (select count(*) from active_like_rows
                        where activated_at >= :fromLocal and activated_at < :toLocal) as period_like_count,
                    (select count(*) from comment_rows) as comment_count,
                    (select count(*) from reply_rows) as reply_count,
                    (select count(*) from share_rows) as share_count,
                    (select count(*) from watch_rows) as watch_session_count,
                    (select count(distinct username) from watch_rows) as unique_viewer_count,
                    (select coalesce(sum(coalesce(duration_watched, 0)), 0) from watch_rows) as watch_seconds,
                    (select avg(duration_watched) from watch_rows where duration_watched is not null)
                        as average_watch_seconds,
                    (select count(*) from watch_rows where completion_status = true) as completed_view_count
                """;

        return jdbc.queryForObject(sql, params(username, range), (rs, rowNum) -> new SummaryMetrics(
                rs.getLong("impression_count"),
                rs.getLong("unique_audience_count"),
                rs.getLong("active_like_count"),
                rs.getLong("period_like_count"),
                rs.getLong("comment_count"),
                rs.getLong("reply_count"),
                rs.getLong("share_count"),
                rs.getLong("watch_session_count"),
                rs.getLong("unique_viewer_count"),
                rs.getLong("watch_seconds"),
                nullableDouble(rs.getObject("average_watch_seconds")),
                rs.getLong("completed_view_count")
        ));
    }

    public List<DailyMetric> loadDailyMetrics(String username, DateRange range) {
        String sql = ACTIVE_CONTENT_CTES + """
                , owned_content as (
                    select 'VIDEO' as content_type, store_id as content_id from active_videos
                    union all
                    select 'IMAGE' as content_type, feed_no as content_id from active_images
                ),
                impression_rows as (
                    select i.*,
                           case
                               when nullif(trim(i.username), '') is not null
                                   then 'u:' || trim(i.username)
                               when nullif(trim(i.guest_id), '') is not null
                                   then 'g:' || trim(i.guest_id)
                               when nullif(trim(i.device_id), '') is not null
                                   then 'd:' || trim(i.device_id)
                               when nullif(trim(i.session_id), '') is not null
                                   then 's:' || trim(i.session_id)
                               else null
                           end as actor_key
                    from fp_376 i
                    join owned_content o
                      on (o.content_type = 'VIDEO' and i.content_type = 'VIDEO' and i.store_id = o.content_id)
                      or (o.content_type = 'IMAGE' and i.content_type = 'IMAGE' and i.feed_no = o.content_id)
                    where i.impressed_at >= :fromLocal and i.impressed_at < :toLocal
                )
                select cast(impressed_at as date) as bucket, 'IMPRESSION' as metric, count(*) as metric_value
                from impression_rows
                group by cast(impressed_at as date)

                union all

                select cast(impressed_at as date) as bucket, 'UNIQUE_AUDIENCE' as metric,
                       count(distinct actor_key) as metric_value
                from impression_rows
                where actor_key is not null
                group by cast(impressed_at as date)

                union all

                select cast(e.server_event_at as date) as bucket, 'VIDEO_PLAY_START' as metric,
                       count(*) as metric_value
                from fp_370 e
                join active_videos v on v.store_id = e.store_id
                where upper(e.event_type) = 'PLAY_START'
                  and e.server_event_at >= :fromLocal and e.server_event_at < :toLocal
                group by cast(e.server_event_at as date)

                union all

                select cast(w.timestamp at time zone 'Asia/Seoul' as date) as bucket,
                       'VIDEO_COMPLETE' as metric, count(*) as metric_value
                from fp_305 w
                join active_videos v on v.store_id = w.store_id
                where w.use_yn = 'Y' and w.deleted_at is null
                  and w.completion_status = true
                  and w.timestamp >= :fromInstant and w.timestamp < :toInstant
                group by cast(w.timestamp at time zone 'Asia/Seoul' as date)

                union all

                select cast(w.timestamp at time zone 'Asia/Seoul' as date) as bucket,
                       'WATCH_SECONDS' as metric,
                       coalesce(sum(coalesce(w.duration_watched, 0)), 0) as metric_value
                from fp_305 w
                join active_videos v on v.store_id = w.store_id
                where w.use_yn = 'Y' and w.deleted_at is null
                  and w.timestamp >= :fromInstant and w.timestamp < :toInstant
                group by cast(w.timestamp at time zone 'Asia/Seoul' as date)

                union all

                select cast(activated_at as date) as bucket, 'NEW_ACTIVE_LIKE' as metric,
                       count(*) as metric_value
                from (
                    select coalesce(l.updated_at, l.created_at) as activated_at
                    from fp_50 l
                    join active_videos v on v.store_id = l.store_id
                    where l.use_yn = 'Y' and l.deleted_at is null
                      and l.username <> :username
                    union all
                    select l.created_at as activated_at
                    from fp_60 l
                    join active_images i on i.feed_no = l.feed_id
                    where l.use_yn = 'Y' and l.deleted_at is null
                      and l.username <> :username
                ) likes
                where activated_at >= :fromLocal and activated_at < :toLocal
                group by cast(activated_at as date)

                union all

                select cast(created_at as date) as bucket, 'COMMENT' as metric, count(*) as metric_value
                from (
                    select c.created_at
                    from fp_440 c
                    join active_videos v on v.store_id = c.store_id
                    where c.use_yn = 'Y' and c.deleted_at is null and c.username <> :username
                    union all
                    select c.created_at
                    from fp_460 c
                    join active_images i on i.feed_no = c.feed_id
                    where c.use_yn = 'Y' and c.deleted_at is null and c.username <> :username
                ) comments
                where created_at >= :fromLocal and created_at < :toLocal
                group by cast(created_at as date)

                union all

                select cast(created_at as date) as bucket, 'REPLY' as metric, count(*) as metric_value
                from (
                    select r.created_at
                    from fp_450 r
                    join fp_440 c on c.comment_id = r.comment_id
                    join active_videos v on v.store_id = c.store_id
                    where r.use_yn = 'Y' and r.deleted_at is null
                      and c.use_yn = 'Y' and c.deleted_at is null
                      and r.username <> :username
                    union all
                    select r.created_at
                    from fp_470 r
                    join fp_460 c on c.comment_id = r.comment_id
                    join active_images i on i.feed_no = c.feed_id
                    where r.use_yn = 'Y' and r.deleted_at is null
                      and c.use_yn = 'Y' and c.deleted_at is null
                      and r.username <> :username
                ) replies
                where created_at >= :fromLocal and created_at < :toLocal
                group by cast(created_at as date)

                union all

                select cast(e.server_event_at as date) as bucket, 'SHARE' as metric, count(*) as metric_value
                from fp_370 e
                join active_videos v on v.store_id = e.store_id
                where upper(e.event_type) = 'SHARE'
                  and (e.username is null or e.username <> :username)
                  and e.server_event_at >= :fromLocal and e.server_event_at < :toLocal
                group by cast(e.server_event_at as date)
                """;

        return jdbc.query(sql, params(username, range), (rs, rowNum) -> new DailyMetric(
                toLocalDate(rs.getObject("bucket")),
                rs.getString("metric"),
                rs.getLong("metric_value")
        ));
    }

    public long countContents(String username, String type) {
        String sql = ACTIVE_CONTENT_CTES + """
                , owned as (
                    select 'VIDEO' as content_type from active_videos
                    union all
                    select 'IMAGE' as content_type from active_images
                )
                select count(*)
                from owned
                where (:type = 'all' or lower(content_type) = :type)
                """;
        Number count = jdbc.queryForObject(sql, new MapSqlParameterSource()
                .addValue("username", username)
                .addValue("type", type), Number.class);
        return count == null ? 0L : count.longValue();
    }

    public List<ContentMetricRow> findContents(
            String username,
            DateRange range,
            String type,
            String sort,
            int page,
            int size
    ) {
        String orderBy = switch (sort) {
            case "views" -> "watch_session_count desc nulls last";
            case "likes" -> "period_like_count desc";
            case "comments" -> "(comment_count + reply_count) desc";
            case "recent" -> "published_on desc nulls last";
            default -> "impression_count desc";
        };

        String sql = ACTIVE_CONTENT_CTES + """
                , owned as (
                    select
                        'VIDEO' as content_type,
                        v.store_id as numeric_id,
                        coalesce(nullif(trim(v.title), ''), nullif(trim(v.store_name), ''), '') as title,
                        v.thumbnail as thumbnail,
                        cast(null as varchar) as images,
                        v.created_at as published_on,
                        case
                            when upper(trim(v.open_yn)) = 'Y' then 'PUBLIC'
                            when upper(trim(v.open_yn)) = 'N' then 'PRIVATE'
                            else 'UNKNOWN'
                        end as visibility
                    from active_videos v
                    union all
                    select
                        'IMAGE' as content_type,
                        i.feed_no as numeric_id,
                        coalesce(nullif(trim(i.feed_title), ''), nullif(trim(i.store_name), ''), '') as title,
                        i.thumbnail as thumbnail,
                        i.images as images,
                        cast(i.created_at as date) as published_on,
                        case
                            when upper(trim(i.open_yn)) = 'Y' then 'PUBLIC'
                            when upper(trim(i.open_yn)) = 'N' then 'PRIVATE'
                            else 'UNKNOWN'
                        end as visibility
                    from active_images i
                ),
                measured as (
                    select
                        o.*,
                        (select count(*)
                         from fp_376 x
                         where ((o.content_type = 'VIDEO' and x.content_type = 'VIDEO' and x.store_id = o.numeric_id)
                             or (o.content_type = 'IMAGE' and x.content_type = 'IMAGE' and x.feed_no = o.numeric_id))
                           and x.impressed_at >= :fromLocal and x.impressed_at < :toLocal) as impression_count,
                        (select count(distinct case
                                    when nullif(trim(x.username), '') is not null then 'u:' || trim(x.username)
                                    when nullif(trim(x.guest_id), '') is not null then 'g:' || trim(x.guest_id)
                                    when nullif(trim(x.device_id), '') is not null then 'd:' || trim(x.device_id)
                                    when nullif(trim(x.session_id), '') is not null then 's:' || trim(x.session_id)
                                    else null end)
                         from fp_376 x
                         where ((o.content_type = 'VIDEO' and x.content_type = 'VIDEO' and x.store_id = o.numeric_id)
                             or (o.content_type = 'IMAGE' and x.content_type = 'IMAGE' and x.feed_no = o.numeric_id))
                           and x.impressed_at >= :fromLocal and x.impressed_at < :toLocal) as unique_audience_count,
                        case when o.content_type = 'VIDEO' then
                            (select count(*) from fp_50 l
                             where l.store_id = o.numeric_id and l.use_yn = 'Y' and l.deleted_at is null
                               and l.username <> :username)
                        else
                            (select count(*) from fp_60 l
                             where l.feed_id = o.numeric_id and l.use_yn = 'Y' and l.deleted_at is null
                               and l.username <> :username)
                        end as active_like_count,
                        case when o.content_type = 'VIDEO' then
                            (select count(*) from fp_50 l
                             where l.store_id = o.numeric_id and l.use_yn = 'Y' and l.deleted_at is null
                               and l.username <> :username
                               and coalesce(l.updated_at, l.created_at) >= :fromLocal
                               and coalesce(l.updated_at, l.created_at) < :toLocal)
                        else
                            (select count(*) from fp_60 l
                             where l.feed_id = o.numeric_id and l.use_yn = 'Y' and l.deleted_at is null
                               and l.username <> :username
                               and l.created_at >= :fromLocal and l.created_at < :toLocal)
                        end as period_like_count,
                        case when o.content_type = 'VIDEO' then
                            (select count(*) from fp_440 c
                             where c.store_id = o.numeric_id and c.use_yn = 'Y' and c.deleted_at is null
                               and c.username <> :username
                               and c.created_at >= :fromLocal and c.created_at < :toLocal)
                        else
                            (select count(*) from fp_460 c
                             where c.feed_id = o.numeric_id and c.use_yn = 'Y' and c.deleted_at is null
                               and c.username <> :username
                               and c.created_at >= :fromLocal and c.created_at < :toLocal)
                        end as comment_count,
                        case when o.content_type = 'VIDEO' then
                            (select count(*) from fp_450 r
                             join fp_440 c on c.comment_id = r.comment_id
                             where c.store_id = o.numeric_id
                               and r.use_yn = 'Y' and r.deleted_at is null
                               and c.use_yn = 'Y' and c.deleted_at is null
                               and r.username <> :username
                               and r.created_at >= :fromLocal and r.created_at < :toLocal)
                        else
                            (select count(*) from fp_470 r
                             join fp_460 c on c.comment_id = r.comment_id
                             where c.feed_id = o.numeric_id
                               and r.use_yn = 'Y' and r.deleted_at is null
                               and c.use_yn = 'Y' and c.deleted_at is null
                               and r.username <> :username
                               and r.created_at >= :fromLocal and r.created_at < :toLocal)
                        end as reply_count,
                        case when o.content_type = 'VIDEO' then
                            (select count(*) from fp_305 w
                             where w.store_id = o.numeric_id and w.use_yn = 'Y' and w.deleted_at is null
                               and w.timestamp >= :fromInstant and w.timestamp < :toInstant)
                        else cast(null as bigint) end as watch_session_count,
                        case when o.content_type = 'VIDEO' then
                            (select count(distinct w.username) from fp_305 w
                             where w.store_id = o.numeric_id and w.use_yn = 'Y' and w.deleted_at is null
                               and w.timestamp >= :fromInstant and w.timestamp < :toInstant)
                        else cast(null as bigint) end as unique_viewer_count,
                        case when o.content_type = 'VIDEO' then
                            (select coalesce(sum(coalesce(w.duration_watched, 0)), 0) from fp_305 w
                             where w.store_id = o.numeric_id and w.use_yn = 'Y' and w.deleted_at is null
                               and w.timestamp >= :fromInstant and w.timestamp < :toInstant)
                        else cast(null as bigint) end as watch_seconds,
                        case when o.content_type = 'VIDEO' then
                            (select avg(w.duration_watched) from fp_305 w
                             where w.store_id = o.numeric_id and w.use_yn = 'Y' and w.deleted_at is null
                               and w.duration_watched is not null
                               and w.timestamp >= :fromInstant and w.timestamp < :toInstant)
                        else cast(null as numeric) end as average_watch_seconds,
                        case when o.content_type = 'VIDEO' then
                            (select count(*) from fp_305 w
                             where w.store_id = o.numeric_id and w.use_yn = 'Y' and w.deleted_at is null
                               and w.completion_status = true
                               and w.timestamp >= :fromInstant and w.timestamp < :toInstant)
                        else cast(null as bigint) end as completed_view_count,
                        case when o.content_type = 'VIDEO' then
                            (select count(*) from fp_370 e
                             where e.store_id = o.numeric_id and upper(e.event_type) = 'SHARE'
                               and (e.username is null or e.username <> :username)
                               and e.server_event_at >= :fromLocal and e.server_event_at < :toLocal)
                        else cast(null as bigint) end as share_count
                    from owned o
                    where (:type = 'all' or lower(o.content_type) = :type)
                )
                select *
                from measured
                """ + "order by " + orderBy + ", published_on desc nulls last, content_type, numeric_id desc\n" +
                "limit :size offset :offset";

        MapSqlParameterSource queryParams = params(username, range)
                .addValue("type", type)
                .addValue("size", size)
                .addValue("offset", (long) page * size);

        return jdbc.query(sql, queryParams, (rs, rowNum) -> new ContentMetricRow(
                rs.getString("content_type"),
                rs.getInt("numeric_id"),
                rs.getString("title"),
                rs.getString("thumbnail"),
                rs.getString("images"),
                toLocalDate(rs.getObject("published_on")),
                rs.getString("visibility"),
                rs.getLong("impression_count"),
                rs.getLong("unique_audience_count"),
                rs.getLong("active_like_count"),
                rs.getLong("period_like_count"),
                rs.getLong("comment_count"),
                rs.getLong("reply_count"),
                nullableLong(rs.getObject("watch_session_count")),
                nullableLong(rs.getObject("unique_viewer_count")),
                nullableLong(rs.getObject("watch_seconds")),
                nullableDouble(rs.getObject("average_watch_seconds")),
                nullableLong(rs.getObject("completed_view_count")),
                nullableLong(rs.getObject("share_count"))
        ));
    }

    private MapSqlParameterSource params(String username, DateRange range) {
        return new MapSqlParameterSource()
                .addValue("username", username)
                .addValue("fromLocal", range.fromLocal())
                .addValue("toLocal", range.toLocal())
                .addValue("fromInstant", range.fromInstant())
                .addValue("toInstant", range.toInstant());
    }

    private static Double nullableDouble(Object value) {
        return value instanceof Number number ? number.doubleValue() : null;
    }

    private static Long nullableLong(Object value) {
        return value instanceof Number number ? number.longValue() : null;
    }

    private static LocalDate toLocalDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof Date date) {
            return date.toLocalDate();
        }
        return LocalDate.parse(value.toString());
    }

    public record DateRange(
            LocalDate from,
            LocalDate to,
            LocalDateTime fromLocal,
            LocalDateTime toLocal,
            OffsetDateTime fromInstant,
            OffsetDateTime toInstant
    ) {
        public static DateRange of(LocalDate from, LocalDate to) {
            LocalDate exclusiveTo = to.plusDays(1);
            return new DateRange(
                    from,
                    to,
                    from.atStartOfDay(),
                    exclusiveTo.atStartOfDay(),
                    from.atStartOfDay(SEOUL).toOffsetDateTime(),
                    exclusiveTo.atStartOfDay(SEOUL).toOffsetDateTime()
            );
        }
    }

    public record ContentCounts(
            long totalCount,
            long videoCount,
            long imageCount,
            long publicCount,
            long privateCount,
            long unknownVisibilityCount
    ) {
    }

    public record SummaryMetrics(
            long impressionCount,
            long uniqueAudienceCount,
            long activeLikeCount,
            long periodLikeCount,
            long commentCount,
            long replyCount,
            long shareCount,
            long watchSessionCount,
            long uniqueViewerCount,
            long watchSeconds,
            Double averageWatchSeconds,
            long completedViewCount
    ) {
    }

    public record DailyMetric(LocalDate date, String metric, long value) {
    }

    public record ContentMetricRow(
            String contentType,
            int numericId,
            String title,
            String thumbnail,
            String images,
            LocalDate publishedOn,
            String visibility,
            long impressionCount,
            long uniqueAudienceCount,
            long activeLikeCount,
            long periodLikeCount,
            long commentCount,
            long replyCount,
            Long watchSessionCount,
            Long uniqueViewerCount,
            Long watchSeconds,
            Double averageWatchSeconds,
            Long completedViewCount,
            Long shareCount
    ) {
    }
}
