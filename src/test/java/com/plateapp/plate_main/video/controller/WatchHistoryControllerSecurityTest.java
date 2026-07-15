package com.plateapp.plate_main.video.controller;

import static org.mockito.Mockito.verify;

import com.plateapp.plate_main.video.dto.WatchHistoryDto;
import com.plateapp.plate_main.video.service.WatchHistoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class WatchHistoryControllerSecurityTest {

    @Mock private WatchHistoryService watchHistoryService;

    @Test
    void updateProgressPassesAuthenticatedUsernameToService() {
        WatchHistoryController controller = new WatchHistoryController(watchHistoryService);
        WatchHistoryDto.UpdateProgressRequest request = new WatchHistoryDto.UpdateProgressRequest();

        controller.updateProgress(10, request, auth("watcher"));

        verify(watchHistoryService).updateProgress("watcher", 10, request);
    }

    @Test
    void completeWatchPassesAuthenticatedUsernameToService() {
        WatchHistoryController controller = new WatchHistoryController(watchHistoryService);
        WatchHistoryDto.CompleteWatchRequest request = new WatchHistoryDto.CompleteWatchRequest();

        controller.completeWatch(10, request, auth("watcher"));

        verify(watchHistoryService).completeWatch("watcher", 10, request);
    }

    private Authentication auth(String username) {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(username, null);
        authentication.setAuthenticated(true);
        return authentication;
    }
}
