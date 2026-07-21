package com.plateapp.plate_main.contentanalytics.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.plateapp.plate_main.contentanalytics.repository.ContentAnalyticsQueryRepository.ContentCounts;
import com.plateapp.plate_main.contentanalytics.repository.ContentAnalyticsQueryRepository.ContentMetricRow;
import com.plateapp.plate_main.contentanalytics.repository.ContentAnalyticsQueryRepository.DailyMetric;
import com.plateapp.plate_main.contentanalytics.repository.ContentAnalyticsQueryRepository.DateRange;
import com.plateapp.plate_main.contentanalytics.repository.ContentAnalyticsQueryRepository.SummaryMetrics;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@JdbcTest
@ActiveProfiles("test")
@Import(ContentAnalyticsQueryRepository.class)
class ContentAnalyticsQueryRepositoryTest {

    private static final LocalDate FROM = LocalDate.of(2026, 7, 1);
    private static final LocalDate TO = LocalDate.of(2026, 7, 2);

    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private ContentAnalyticsQueryRepository repository;

    @BeforeEach
    void setUp() {
        jdbc.execute("drop all objects");
        // Keep H2's TIMESTAMP WITH TIME ZONE date buckets aligned with the API contract.
        jdbc.execute("set time zone 'Asia/Seoul'");
        createTables();
        insertContents();
        insertAnalytics();
    }

    @Test
    void summaryUsesCanonicalSourcesAndExcludesAuthorsOwnReactions() {
        ContentCounts counts = repository.loadContentCounts("author");
        SummaryMetrics metrics = repository.loadSummaryMetrics("author", DateRange.of(FROM, TO));

        assertThat(counts).isEqualTo(new ContentCounts(3, 2, 1, 1, 1, 1));
        assertThat(metrics.impressionCount()).isEqualTo(3);
        assertThat(metrics.uniqueAudienceCount()).isEqualTo(2);
        assertThat(metrics.activeLikeCount()).isEqualTo(3);
        assertThat(metrics.periodLikeCount()).isEqualTo(2);
        assertThat(metrics.commentCount()).isEqualTo(2);
        assertThat(metrics.replyCount()).isEqualTo(2);
        assertThat(metrics.shareCount()).isEqualTo(2);
        assertThat(metrics.watchSessionCount()).isEqualTo(3);
        assertThat(metrics.uniqueViewerCount()).isEqualTo(2);
        assertThat(metrics.watchSeconds()).isEqualTo(60);
        assertThat(metrics.averageWatchSeconds()).isEqualTo(20.0);
        assertThat(metrics.completedViewCount()).isEqualTo(2);
    }

    @Test
    void trendsSeparatePlayEventsFromCompletedWatchSessionsAndFillSourceBuckets() {
        List<DailyMetric> rows = repository.loadDailyMetrics("author", DateRange.of(FROM, TO));
        Map<String, Long> metrics = rows.stream().collect(Collectors.toMap(
                row -> row.date() + ":" + row.metric(),
                DailyMetric::value
        ));

        assertThat(metrics.get("2026-07-01:IMPRESSION")).isEqualTo(2);
        assertThat(metrics.get("2026-07-01:UNIQUE_AUDIENCE")).isEqualTo(1);
        assertThat(metrics.get("2026-07-01:VIDEO_PLAY_START")).isEqualTo(1);
        assertThat(metrics.get("2026-07-01:VIDEO_COMPLETE")).isEqualTo(1);
        assertThat(metrics.get("2026-07-01:WATCH_SECONDS")).isEqualTo(40);
        assertThat(metrics.get("2026-07-01:NEW_ACTIVE_LIKE")).isEqualTo(1);
        assertThat(metrics.get("2026-07-01:COMMENT")).isEqualTo(1);

        assertThat(metrics.get("2026-07-02:IMPRESSION")).isEqualTo(1);
        assertThat(metrics.get("2026-07-02:VIDEO_PLAY_START")).isEqualTo(1);
        assertThat(metrics.get("2026-07-02:VIDEO_COMPLETE")).isEqualTo(1);
        assertThat(metrics.get("2026-07-02:WATCH_SECONDS")).isEqualTo(20);
        assertThat(metrics.get("2026-07-02:NEW_ACTIVE_LIKE")).isEqualTo(1);
        assertThat(metrics.get("2026-07-02:COMMENT")).isEqualTo(1);
        assertThat(metrics.get("2026-07-02:REPLY")).isEqualTo(2);
        assertThat(metrics.get("2026-07-02:SHARE")).isEqualTo(2);
    }

    @Test
    void contentsApplyTypeMetricsAndViewsSortingBeforePaging() {
        long total = repository.countContents("author", "all");
        List<ContentMetricRow> rows = repository.findContents(
                "author", DateRange.of(FROM, TO), "all", "views", 0, 10);

        assertThat(total).isEqualTo(3);
        assertThat(rows).extracting(ContentMetricRow::contentType)
                .containsExactly("VIDEO", "VIDEO", "IMAGE");
        assertThat(rows).extracting(ContentMetricRow::numericId)
                .containsExactly(1, 2, 10);

        ContentMetricRow firstVideo = rows.get(0);
        assertThat(firstVideo.watchSessionCount()).isEqualTo(2);
        assertThat(firstVideo.activeLikeCount()).isEqualTo(1);
        assertThat(firstVideo.commentCount()).isEqualTo(1);
        assertThat(firstVideo.replyCount()).isEqualTo(1);
        assertThat(firstVideo.shareCount()).isEqualTo(1);

        ContentMetricRow image = rows.get(2);
        assertThat(image.visibility()).isEqualTo("UNKNOWN");
        assertThat(image.images()).isEqualTo("first.jpg,,second.jpg");
        assertThat(image.watchSessionCount()).isNull();
        assertThat(image.shareCount()).isNull();

        assertThat(repository.countContents("author", "image")).isEqualTo(1);
        assertThat(repository.findContents(
                "author", DateRange.of(FROM, TO), "image", "recent", 0, 10))
                .extracting(ContentMetricRow::numericId)
                .containsExactly(10);
    }

    private void createTables() {
        jdbc.execute("""
                create table fp_300 (
                    store_id integer primary key,
                    username varchar(50) not null,
                    title varchar(255),
                    store_name varchar(255),
                    thumbnail varchar(500),
                    created_at date,
                    open_yn char(1),
                    use_yn char(1) not null,
                    deleted_at date
                )
                """);
        jdbc.execute("""
                create table fp_400 (
                    feed_no integer primary key,
                    username varchar(50) not null,
                    feed_title varchar(255),
                    store_name varchar(255),
                    thumbnail varchar(500),
                    images varchar(2000),
                    created_at timestamp,
                    open_yn char(1),
                    use_yn char(1) not null
                )
                """);
        jdbc.execute("""
                create table fp_376 (
                    impression_id bigint primary key,
                    username varchar(50),
                    guest_id varchar(100),
                    device_id varchar(200),
                    session_id varchar(150),
                    content_type varchar(20) not null,
                    store_id integer,
                    feed_no integer,
                    impressed_at timestamp not null
                )
                """);
        jdbc.execute("""
                create table fp_305 (
                    id integer primary key,
                    username varchar(50) not null,
                    store_id integer not null,
                    timestamp timestamp with time zone,
                    duration_watched integer,
                    completion_status boolean,
                    use_yn char(1) not null,
                    deleted_at date
                )
                """);
        jdbc.execute("""
                create table fp_370 (
                    event_id bigint primary key,
                    username varchar(50),
                    store_id integer not null,
                    event_type varchar(40) not null,
                    server_event_at timestamp not null
                )
                """);
        jdbc.execute("""
                create table fp_50 (
                    username varchar(50) not null,
                    store_id integer not null,
                    use_yn char(1) not null,
                    deleted_at date,
                    created_at timestamp,
                    updated_at timestamp
                )
                """);
        jdbc.execute("""
                create table fp_60 (
                    username varchar(50) not null,
                    feed_id integer not null,
                    use_yn char(1) not null,
                    deleted_at timestamp,
                    created_at timestamp,
                    updated_at timestamp
                )
                """);
        jdbc.execute("""
                create table fp_440 (
                    comment_id integer primary key,
                    store_id integer not null,
                    username varchar(50) not null,
                    use_yn char(1) not null,
                    deleted_at date,
                    created_at timestamp
                )
                """);
        jdbc.execute("""
                create table fp_450 (
                    reply_id integer primary key,
                    comment_id integer not null,
                    username varchar(50) not null,
                    use_yn char(1) not null,
                    deleted_at timestamp,
                    created_at timestamp
                )
                """);
        jdbc.execute("""
                create table fp_460 (
                    comment_id integer primary key,
                    feed_id integer not null,
                    username varchar(50) not null,
                    use_yn char(1) not null,
                    deleted_at timestamp,
                    created_at timestamp
                )
                """);
        jdbc.execute("""
                create table fp_470 (
                    reply_id integer primary key,
                    comment_id integer not null,
                    username varchar(50) not null,
                    use_yn char(1) not null,
                    deleted_at timestamp,
                    created_at timestamp
                )
                """);
    }

    private void insertContents() {
        jdbc.update("insert into fp_300 values (1, 'author', 'Video 1', 'Store', 'v1.jpg', date '2026-06-01', 'Y', 'Y', null)");
        jdbc.update("insert into fp_300 values (2, 'author', 'Video 2', 'Store', 'v2.jpg', date '2026-06-02', 'N', 'Y', null)");
        jdbc.update("insert into fp_300 values (3, 'author', 'Inactive', 'Store', null, date '2026-06-03', 'Y', 'N', null)");
        jdbc.update("insert into fp_300 values (4, 'other', 'Other', 'Store', null, date '2026-06-04', 'Y', 'Y', null)");
        jdbc.update("insert into fp_400 values (10, 'author', 'Image', 'Store', null, 'first.jpg,,second.jpg', timestamp '2026-06-03 12:00:00', null, 'Y')");
        jdbc.update("insert into fp_400 values (11, 'author', 'Inactive image', 'Store', null, null, timestamp '2026-06-04 12:00:00', 'Y', 'N')");
        jdbc.update("insert into fp_400 values (12, 'other', 'Other image', 'Store', null, null, timestamp '2026-06-05 12:00:00', 'Y', 'Y')");
    }

    private void insertAnalytics() {
        jdbc.update("insert into fp_376 values (1, 'viewer', null, null, 's1', 'VIDEO', 1, null, timestamp '2026-07-01 10:00:00')");
        jdbc.update("insert into fp_376 values (2, 'viewer', null, null, 's1', 'IMAGE', null, 10, timestamp '2026-07-01 11:00:00')");
        jdbc.update("insert into fp_376 values (3, null, 'guest-1', null, 's2', 'VIDEO', 2, null, timestamp '2026-07-02 10:00:00')");
        jdbc.update("insert into fp_376 values (4, 'viewer', null, null, 's1', 'VIDEO', 1, null, timestamp '2026-06-30 23:59:59')");
        jdbc.update("insert into fp_376 values (5, 'viewer', null, null, 's1', 'VIDEO', 4, null, timestamp '2026-07-01 12:00:00')");

        jdbc.update("insert into fp_305 values (1, 'viewer', 1, timestamp with time zone '2026-07-01 01:00:00+09:00', 10, false, 'Y', null)");
        jdbc.update("insert into fp_305 values (2, 'viewer2', 1, timestamp with time zone '2026-07-01 02:00:00+09:00', 30, true, 'Y', null)");
        jdbc.update("insert into fp_305 values (3, 'viewer', 2, timestamp with time zone '2026-07-02 01:00:00+09:00', 20, true, 'Y', null)");

        jdbc.update("insert into fp_50 values ('viewer', 1, 'Y', null, timestamp '2026-07-01 10:00:00', timestamp '2026-07-01 10:00:00')");
        jdbc.update("insert into fp_50 values ('author', 1, 'Y', null, timestamp '2026-07-01 11:00:00', timestamp '2026-07-01 11:00:00')");
        jdbc.update("insert into fp_50 values ('old-viewer', 2, 'Y', null, timestamp '2026-06-01 10:00:00', timestamp '2026-06-01 10:00:00')");
        jdbc.update("insert into fp_50 values ('inactive-viewer', 1, 'N', date '2026-07-01', timestamp '2026-07-01 12:00:00', timestamp '2026-07-01 12:00:00')");
        jdbc.update("insert into fp_60 values ('viewer2', 10, 'Y', null, timestamp '2026-07-02 10:00:00', timestamp '2026-07-02 10:00:00')");
        jdbc.update("insert into fp_60 values ('author', 10, 'Y', null, timestamp '2026-07-02 11:00:00', timestamp '2026-07-02 11:00:00')");

        jdbc.update("insert into fp_440 values (1, 1, 'viewer', 'Y', null, timestamp '2026-07-01 10:00:00')");
        jdbc.update("insert into fp_440 values (2, 1, 'author', 'Y', null, timestamp '2026-07-01 11:00:00')");
        jdbc.update("insert into fp_460 values (10, 10, 'viewer2', 'Y', null, timestamp '2026-07-02 10:00:00')");
        jdbc.update("insert into fp_450 values (1, 1, 'viewer2', 'Y', null, timestamp '2026-07-02 10:00:00')");
        jdbc.update("insert into fp_450 values (2, 1, 'author', 'Y', null, timestamp '2026-07-02 11:00:00')");
        jdbc.update("insert into fp_470 values (10, 10, 'viewer', 'Y', null, timestamp '2026-07-02 12:00:00')");

        jdbc.update("insert into fp_370 values (1, 'viewer', 1, 'PLAY_START', timestamp '2026-07-01 10:00:00')");
        jdbc.update("insert into fp_370 values (2, 'viewer', 2, 'PLAY_START', timestamp '2026-07-02 10:00:00')");
        jdbc.update("insert into fp_370 values (3, 'viewer', 1, 'PLAY_COMPLETE', timestamp '2026-07-01 10:01:00')");
        jdbc.update("insert into fp_370 values (4, 'viewer', 1, 'IMPRESSION', timestamp '2026-07-01 09:59:00')");
        jdbc.update("insert into fp_370 values (5, 'viewer', 1, 'SHARE', timestamp '2026-07-02 10:00:00')");
        jdbc.update("insert into fp_370 values (6, 'author', 1, 'SHARE', timestamp '2026-07-02 11:00:00')");
        jdbc.update("insert into fp_370 values (7, null, 2, 'SHARE', timestamp '2026-07-02 12:00:00')");
    }
}
