package com.plateapp.plate_main.mypage.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import com.plateapp.plate_main.common.error.GlobalExceptionHandler;
import com.plateapp.plate_main.common.filter.RequestIdFilter;
import com.plateapp.plate_main.mypage.dto.MyHubResponse;
import com.plateapp.plate_main.mypage.dto.MyHubResponse.ContentPreview;
import com.plateapp.plate_main.mypage.dto.MyHubResponse.ContentType;
import com.plateapp.plate_main.mypage.dto.MyHubResponse.TimePrecision;
import com.plateapp.plate_main.mypage.service.MyHubService;
import java.time.Instant;
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
class MyHubHttpContractTest {

    @Mock
    private MyHubService myHubService;

    private MockMvc mockMvc;
    private TestingAuthenticationToken authentication;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new MyHubController(myHubService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new RequestIdFilter())
                .build();
        authentication = new TestingAuthenticationToken("me", null);
        authentication.setAuthenticated(true);
    }

    @Test
    void successUsesFlatEnvelopeAndSerializesContractNulls() throws Exception {
        MyHubResponse response = new MyHubResponse(
                new MyHubResponse.Profile("me", "Me", null, null, false),
                new MyHubResponse.Counts(1, 1, 0, 0, 0, 0, 0),
                List.of(MyHubResponse.Section.RECENT_CONTENT),
                List.of(new ContentPreview(
                        ContentType.VIDEO,
                        "video:81",
                        81,
                        null,
                        null,
                        "Legacy video",
                        null,
                        null,
                        new MyHubResponse.Author("me", "Me", null),
                        null,
                        LocalDate.of(2026, 7, 15),
                        TimePrecision.DATE,
                        null,
                        null,
                        null
                )),
                List.of(),
                null,
                Instant.parse("2026-07-16T09:00:00Z")
        );
        when(myHubService.getHub("me", 3)).thenReturn(response);

        mockMvc.perform(get("/api/my/hub")
                        .principal(authentication)
                        .header(RequestIdFilter.HEADER_REQUEST_ID, "hub-contract-1"))
                .andExpect(status().isOk())
                .andExpect(header().string(RequestIdFilter.HEADER_REQUEST_ID, "hub-contract-1"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.requestId").value("hub-contract-1"))
                .andExpect(jsonPath("$.message").doesNotExist())
                .andExpect(jsonPath("$.errorCode").doesNotExist())
                .andExpect(jsonPath("$.data.profile.profileImageUrl").value(nullValue()))
                .andExpect(jsonPath("$.data.profile.activeRegion").value(nullValue()))
                .andExpect(jsonPath("$.data.primaryAction").value(nullValue()))
                .andExpect(jsonPath("$.data.recentContent[0].imageFeedId").value(nullValue()))
                .andExpect(jsonPath("$.data.recentContent[0].store").value(nullValue()))
                .andExpect(jsonPath("$.data.recentContent[0].createdAt").value(nullValue()))
                .andExpect(jsonPath("$.data.recentContent[0].likedTimePrecision").value(nullValue()));
    }

    @Test
    void blankPreviewLimitUsesExactCommon400Envelope() throws Exception {
        mockMvc.perform(get("/api/my/hub")
                        .principal(authentication)
                        .queryParam("previewLimit", "")
                        .header(RequestIdFilter.HEADER_REQUEST_ID, "hub-contract-400"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("COMMON_400"))
                .andExpect(jsonPath("$.message").value("previewLimit는 0 이상 6 이하여야 합니다."))
                .andExpect(jsonPath("$.requestId").value("hub-contract-400"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void disabledFeatureUsesServiceUnavailableEnvelope() throws Exception {
        when(myHubService.getHub("me", 3))
                .thenThrow(new AppException(ErrorCode.MY_HUB_FEATURE_DISABLED));

        mockMvc.perform(get("/api/my/hub")
                        .principal(authentication)
                        .header(RequestIdFilter.HEADER_REQUEST_ID, "hub-contract-503"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("MY_HUB_FEATURE_DISABLED"))
                .andExpect(jsonPath("$.requestId").value("hub-contract-503"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
