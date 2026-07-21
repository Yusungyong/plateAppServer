package com.plateapp.plate_main.contentanalytics.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import com.plateapp.plate_main.common.error.GlobalExceptionHandler;
import com.plateapp.plate_main.common.filter.RequestIdFilter;
import com.plateapp.plate_main.contentanalytics.dto.ContentAnalyticsDtos;
import com.plateapp.plate_main.contentanalytics.service.ContentAnalyticsService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ContentAnalyticsHttpContractTest {

    private static final LocalDate FROM = LocalDate.of(2026, 7, 1);
    private static final LocalDate TO = LocalDate.of(2026, 7, 21);

    @Mock
    private ContentAnalyticsService service;

    private MockMvc mockMvc;
    private TestingAuthenticationToken authentication;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ContentAnalyticsController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new RequestIdFilter())
                .build();
        authentication = new TestingAuthenticationToken("author", null);
        authentication.setAuthenticated(true);
    }

    @Test
    void summaryUsesDocumentedEnvelopeAndNullableRates() throws Exception {
        ContentAnalyticsDtos.SummaryResponse response = new ContentAnalyticsDtos.SummaryResponse(
                new ContentAnalyticsDtos.Period(FROM, TO, "Asia/Seoul"),
                new ContentAnalyticsDtos.ContentSummary(0, 0, 0, 0, 0, 0),
                new ContentAnalyticsDtos.ExposureSummary(0, 0),
                new ContentAnalyticsDtos.EngagementSummary(0, 0, 0, 0, 0, null),
                new ContentAnalyticsDtos.VideoSummary(0, 0, 0, null, 0, null)
        );
        when(service.summary("author", FROM, TO)).thenReturn(response);

        mockMvc.perform(get("/api/my/content-analytics/summary")
                        .principal(authentication)
                        .queryParam("from", "2026-07-01")
                        .queryParam("to", "2026-07-21")
                        .header(RequestIdFilter.HEADER_REQUEST_ID, "analytics-summary-1"))
                .andExpect(status().isOk())
                .andExpect(header().string(RequestIdFilter.HEADER_REQUEST_ID, "analytics-summary-1"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.requestId").value("analytics-summary-1"))
                .andExpect(jsonPath("$.data.period.timezone").value("Asia/Seoul"))
                .andExpect(jsonPath("$.data.content.unknownVisibilityCount").value(0))
                .andExpect(jsonPath("$.data.engagement.engagementRate").value(nullValue()))
                .andExpect(jsonPath("$.data.video.averageWatchSeconds").value(nullValue()))
                .andExpect(jsonPath("$.data.video.completionRate").value(nullValue()));
    }

    @Test
    void contentsSerializesImageVideoMetricsAsNull() throws Exception {
        ContentAnalyticsDtos.ContentItem image = new ContentAnalyticsDtos.ContentItem(
                "IMAGE",
                "image:8",
                null,
                8,
                "Image",
                null,
                null,
                "UNKNOWN",
                2,
                new ContentAnalyticsDtos.ContentMetrics(10, 7, 2, 1, 1, 0, 0.2),
                null
        );
        when(service.contents(eq("author"), eq(FROM), eq(TO), eq("image"), eq("recent"), eq(0), eq(20)))
                .thenReturn(new ContentAnalyticsDtos.ContentPageResponse(
                        List.of(image), 0, 20, 1, 1, false));

        mockMvc.perform(get("/api/my/content-analytics/contents")
                        .principal(authentication)
                        .queryParam("from", "2026-07-01")
                        .queryParam("to", "2026-07-21")
                        .queryParam("type", "image")
                        .queryParam("sort", "recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].contentType").value("IMAGE"))
                .andExpect(jsonPath("$.data.content[0].videoStoreId").value(nullValue()))
                .andExpect(jsonPath("$.data.content[0].imageFeedId").value(8))
                .andExpect(jsonPath("$.data.content[0].publishedOn").value(nullValue()))
                .andExpect(jsonPath("$.data.content[0].visibility").value("UNKNOWN"))
                .andExpect(jsonPath("$.data.content[0].videoMetrics").value(nullValue()));
    }

    @Test
    void missingDateUsesMissingParameterErrorCode() throws Exception {
        mockMvc.perform(get("/api/my/content-analytics/summary")
                        .principal(authentication)
                        .queryParam("to", "2026-07-21"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("COMMON_401"));
    }

    @Test
    void malformedDateUsesTypeMismatchErrorCode() throws Exception {
        mockMvc.perform(get("/api/my/content-analytics/summary")
                        .principal(authentication)
                        .queryParam("from", "2026-07-XX")
                        .queryParam("to", "2026-07-21"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("COMMON_402"));
    }

    @Test
    void unauthenticatedRequestUsesAuth401Envelope() throws Exception {
        mockMvc.perform(get("/api/my/content-analytics/summary")
                        .queryParam("from", "2026-07-01")
                        .queryParam("to", "2026-07-21")
                        .header(RequestIdFilter.HEADER_REQUEST_ID, "analytics-auth-1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("AUTH_401"))
                .andExpect(jsonPath("$.requestId").value("analytics-auth-1"));
    }

    @Test
    void serviceValidationErrorUsesCommonEnvelope() throws Exception {
        when(service.trends(eq("author"), eq(FROM), eq(TO), any(String.class)))
                .thenThrow(new AppException(ErrorCode.COMMON_INVALID_INPUT, "date range is invalid."));

        mockMvc.perform(get("/api/my/content-analytics/trends")
                        .principal(authentication)
                        .queryParam("from", "2026-07-01")
                        .queryParam("to", "2026-07-21")
                        .queryParam("interval", "day")
                        .header(RequestIdFilter.HEADER_REQUEST_ID, "analytics-invalid-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("COMMON_400"))
                .andExpect(jsonPath("$.message").value("date range is invalid."))
                .andExpect(jsonPath("$.requestId").value("analytics-invalid-1"));
    }
}
