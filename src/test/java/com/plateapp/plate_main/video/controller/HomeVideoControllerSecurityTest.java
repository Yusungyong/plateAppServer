package com.plateapp.plate_main.video.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.plateapp.plate_main.video.dto.VideoWatchHistoryCreateRequest;
import com.plateapp.plate_main.video.service.HomeVideoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class HomeVideoControllerSecurityTest {

    @Mock
    private HomeVideoService homeVideoService;

    @Test
    void createVideoWatchHistoryUsesAuthenticatedUsername() {
        HomeVideoController controller = new HomeVideoController(homeVideoService);
        VideoWatchHistoryCreateRequest request = new VideoWatchHistoryCreateRequest();
        request.setStoreId(10L);
        request.setUsername("other-user");
        request.setIsGuest(true);
        request.setGuestId("guest-1");

        controller.createVideoWatchHistory(request, auth("me"));

        ArgumentCaptor<VideoWatchHistoryCreateRequest> captor =
                ArgumentCaptor.forClass(VideoWatchHistoryCreateRequest.class);
        verify(homeVideoService).saveWatchHistory(captor.capture());
        VideoWatchHistoryCreateRequest savedRequest = captor.getValue();
        assertThat(savedRequest.getUsername()).isEqualTo("me");
        assertThat(savedRequest.getIsGuest()).isFalse();
        assertThat(savedRequest.getGuestId()).isNull();
    }

    private Authentication auth(String username) {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(username, null);
        authentication.setAuthenticated(true);
        return authentication;
    }
}
