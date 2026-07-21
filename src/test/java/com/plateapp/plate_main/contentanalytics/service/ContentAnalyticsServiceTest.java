package com.plateapp.plate_main.contentanalytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContentAnalyticsServiceTest {

    private static final LocalDate FROM = LocalDate.of(2026, 7, 1);
    private static final LocalDate TO = LocalDate.of(2026, 7, 3);

    @Mock
    private ContentAnalyticsQueryRepository repository;
    @Mock
    private S3UploadService s3UploadService;

    private ContentAnalyticsService service;

    @BeforeEach
    void setUp() {
        service = new ContentAnalyticsService(repository, s3UploadService);
    }

    @Test
    void summaryMapsSnapshotAndPeriodMetricsWithNullableSafeRates() {
        when(repository.loadContentCounts("author"))
                .thenReturn(new ContentCounts(3, 2, 1, 1, 1, 1));
        when(repository.loadSummaryMetrics(eq("author"), any(DateRange.class)))
                .thenReturn(new SummaryMetrics(
                        100,
                        70,
                        20,
                        5,
                        3,
                        1,
                        1,
                        40,
                        30,
                        800,
                        20.0,
                        10
                ));

        ContentAnalyticsDtos.SummaryResponse response = service.summary("author", FROM, TO);

        assertThat(response.period().timezone()).isEqualTo("Asia/Seoul");
        assertThat(response.content().totalCount()).isEqualTo(3);
        assertThat(response.content().unknownVisibilityCount()).isEqualTo(1);
        assertThat(response.engagement().engagementRate()).isEqualTo(0.1);
        assertThat(response.video().averageWatchSeconds()).isEqualTo(20.0);
        assertThat(response.video().completionRate()).isEqualTo(0.25);
    }

    @Test
    void summaryReturnsNullRatesWhenThereIsNoDenominator() {
        when(repository.loadContentCounts("author"))
                .thenReturn(new ContentCounts(0, 0, 0, 0, 0, 0));
        when(repository.loadSummaryMetrics(eq("author"), any(DateRange.class)))
                .thenReturn(new SummaryMetrics(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, null, 0));

        ContentAnalyticsDtos.SummaryResponse response = service.summary("author", FROM, TO);

        assertThat(response.engagement().engagementRate()).isNull();
        assertThat(response.video().averageWatchSeconds()).isNull();
        assertThat(response.video().completionRate()).isNull();
    }

    @Test
    void engagementRateCanExceedOneWhenSeveralReactionsFollowOneImpression() {
        when(repository.loadContentCounts("author"))
                .thenReturn(new ContentCounts(1, 1, 0, 1, 0, 0));
        when(repository.loadSummaryMetrics(eq("author"), any(DateRange.class)))
                .thenReturn(new SummaryMetrics(1, 1, 4, 2, 1, 1, 1, 0, 0, 0, null, 0));

        ContentAnalyticsDtos.SummaryResponse response = service.summary("author", FROM, TO);

        assertThat(response.engagement().engagementRate()).isEqualTo(5.0);
    }

    @Test
    void trendsFillsMissingDaysWithZerosAndMergesMetrics() {
        when(repository.loadDailyMetrics(eq("author"), any(DateRange.class))).thenReturn(List.of(
                new DailyMetric(FROM, "IMPRESSION", 10),
                new DailyMetric(FROM, "UNIQUE_AUDIENCE", 7),
                new DailyMetric(FROM, "VIDEO_PLAY_START", 4),
                new DailyMetric(FROM, "VIDEO_COMPLETE", 2),
                new DailyMetric(FROM, "WATCH_SECONDS", 90),
                new DailyMetric(FROM, "NEW_ACTIVE_LIKE", 3),
                new DailyMetric(TO, "COMMENT", 2),
                new DailyMetric(TO, "REPLY", 1),
                new DailyMetric(TO, "SHARE", 1)
        ));

        ContentAnalyticsDtos.TrendsResponse response = service.trends("author", FROM, TO, "day");

        assertThat(response.points()).hasSize(3);
        assertThat(response.points().get(0).impressionCount()).isEqualTo(10);
        assertThat(response.points().get(0).watchSeconds()).isEqualTo(90);
        assertThat(response.points().get(1).impressionCount()).isZero();
        assertThat(response.points().get(1).commentCount()).isZero();
        assertThat(response.points().get(2).commentCount()).isEqualTo(2);
        assertThat(response.points().get(2).replyCount()).isEqualTo(1);
        assertThat(response.points().get(2).shareCount()).isEqualTo(1);
    }

    @Test
    void trendsAllowsExactlyNinetyThreeInclusiveDays() {
        LocalDate ninetyThirdDay = FROM.plusDays(92);
        when(repository.loadDailyMetrics(eq("author"), any(DateRange.class))).thenReturn(List.of());

        ContentAnalyticsDtos.TrendsResponse response = service.trends(
                "author", FROM, ninetyThirdDay, "day");

        assertThat(response.points()).hasSize(93);
        assertThat(response.points().get(92).date()).isEqualTo(ninetyThirdDay);
    }

    @Test
    void contentsMapsVideoAndImageSpecificFieldsAndPagination() {
        when(repository.countContents("author", "all")).thenReturn(3L);
        when(repository.findContents(eq("author"), any(DateRange.class), eq("all"), eq("impressions"), eq(0), eq(2)))
                .thenReturn(List.of(
                        new ContentMetricRow(
                                "VIDEO", 11, "Video", "video-thumb.jpg", null, FROM, "PUBLIC",
                                100, 70, 12, 4, 2, 1,
                                30L, 20L, 600L, 20.0, 10L, 2L
                        ),
                        new ContentMetricRow(
                                "IMAGE", 21, "Image", null, "first.jpg,,second.jpg", FROM, "UNKNOWN",
                                50, 40, 8, 2, 1, 1,
                                null, null, null, null, null, null
                        )
                ));
        when(s3UploadService.toImageUrl("video-thumb.jpg")).thenReturn("video-url");
        when(s3UploadService.toFeedImageUrl("first.jpg")).thenReturn("image-url");

        ContentAnalyticsDtos.ContentPageResponse response = service.contents(
                "author", FROM, TO, "all", "impressions", 0, 2);

        assertThat(response.totalElements()).isEqualTo(3);
        assertThat(response.totalPages()).isEqualTo(2);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.content()).hasSize(2);

        ContentAnalyticsDtos.ContentItem video = response.content().get(0);
        assertThat(video.contentId()).isEqualTo("video:11");
        assertThat(video.thumbnailUrl()).isEqualTo("video-url");
        assertThat(video.imageCount()).isNull();
        assertThat(video.metrics().engagementRate()).isEqualTo(0.09);
        assertThat(video.videoMetrics().completionRate()).isEqualTo(0.3333);

        ContentAnalyticsDtos.ContentItem image = response.content().get(1);
        assertThat(image.contentId()).isEqualTo("image:21");
        assertThat(image.thumbnailUrl()).isEqualTo("image-url");
        assertThat(image.imageCount()).isEqualTo(2);
        assertThat(image.videoMetrics()).isNull();
        assertThat(image.metrics().engagementRate()).isEqualTo(0.08);
    }

    @Test
    void validatesRangesAndListParametersBeforeQuerying() {
        assertInvalid(() -> service.summary("author", TO, FROM));
        assertInvalid(() -> service.trends("author", FROM, FROM.plusDays(93), "day"));
        assertInvalid(() -> service.trends("author", FROM, TO, "week"));
        assertInvalid(() -> service.contents("author", FROM, TO, "audio", "recent", 0, 20));
        assertInvalid(() -> service.contents("author", FROM, TO, "all", "score", 0, 20));
        assertInvalid(() -> service.contents("author", FROM, TO, "all", "recent", -1, 20));
        assertInvalid(() -> service.contents("author", FROM, TO, "all", "recent", 0, 101));

        verifyNoInteractions(repository, s3UploadService);
    }

    @Test
    void rejectsAnonymousServiceCalls() {
        assertThatThrownBy(() -> service.summary("anonymousUser", FROM, TO))
                .isInstanceOf(AppException.class)
                .extracting(exception -> ((AppException) exception).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_UNAUTHORIZED);
        verifyNoInteractions(repository, s3UploadService);
    }

    private void assertInvalid(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(AppException.class)
                .extracting(exception -> ((AppException) exception).getErrorCode())
                .isEqualTo(ErrorCode.COMMON_INVALID_INPUT);
    }
}
