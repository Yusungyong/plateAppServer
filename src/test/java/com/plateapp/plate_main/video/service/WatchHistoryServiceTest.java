package com.plateapp.plate_main.video.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.plateapp.plate_main.common.s3.S3UploadService;
import com.plateapp.plate_main.user.repository.MemberRepository;
import com.plateapp.plate_main.video.dto.WatchHistoryDto;
import com.plateapp.plate_main.video.entity.Fp305WatchHistory;
import com.plateapp.plate_main.video.repository.Fp300StoreRepository;
import com.plateapp.plate_main.video.repository.Fp305WatchHistoryRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WatchHistoryServiceTest {

    private static final String SESSION_ID = "client-session";
    private static final String USERNAME = "watcher";
    private static final Integer STORE_ID = 10;

    @Mock private Fp305WatchHistoryRepository watchHistoryRepository;
    @Mock private Fp300StoreRepository storeRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private S3UploadService s3UploadService;

    private WatchHistoryService watchHistoryService;

    @BeforeEach
    void setUp() {
        watchHistoryService = new WatchHistoryService(
                watchHistoryRepository,
                storeRepository,
                memberRepository,
                s3UploadService
        );
    }

    @Test
    void updateProgressRejectsSessionOwnedByAnotherUser() {
        WatchHistoryDto.UpdateProgressRequest request = progressRequest();
        when(watchHistoryRepository
                .findFirstBySessionIdAndUsernameAndStoreIdAndUseYnAndDeletedAtIsNullOrderByTimestampDesc(
                        SESSION_ID, USERNAME, STORE_ID, "Y"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> watchHistoryService.updateProgress(USERNAME, STORE_ID, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Active watch session not found");

        verify(watchHistoryRepository, never()).save(org.mockito.ArgumentMatchers.any(Fp305WatchHistory.class));
        verifyNoInteractions(storeRepository);
    }

    @Test
    void completeWatchRejectsSessionForAnotherVideo() {
        WatchHistoryDto.CompleteWatchRequest request = completeRequest();
        when(watchHistoryRepository
                .findFirstBySessionIdAndUsernameAndStoreIdAndUseYnAndDeletedAtIsNullOrderByTimestampDesc(
                        SESSION_ID, USERNAME, STORE_ID, "Y"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> watchHistoryService.completeWatch(USERNAME, STORE_ID, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Active watch session not found");

        verify(watchHistoryRepository, never()).save(org.mockito.ArgumentMatchers.any(Fp305WatchHistory.class));
        verifyNoInteractions(storeRepository);
    }

    @Test
    void getVideoWatchStatsAggregatesQualityAndDeviceDistributions() {
        when(watchHistoryRepository.countByStoreIdAndUseYnAndDeletedAtIsNull(STORE_ID, "Y"))
                .thenReturn(15L);
        when(watchHistoryRepository.countUniqueViewersByStoreId(STORE_ID)).thenReturn(8L);
        when(watchHistoryRepository
                .countByStoreIdAndCompletionStatusAndUseYnAndDeletedAtIsNull(STORE_ID, true, "Y"))
                .thenReturn(6L);
        when(watchHistoryRepository.getAverageDurationByStoreId(STORE_ID)).thenReturn(42.5);
        when(watchHistoryRepository.countVideoQualityByStoreId(STORE_ID)).thenReturn(List.of(
                attribute("1080p", 2L),
                attribute("720P", 3L),
                attribute("360", 4L),
                attribute(null, 5L),
                attribute("adaptive", 1L)
        ));
        when(watchHistoryRepository.countDeviceInfoByStoreId(STORE_ID)).thenReturn(List.of(
                attribute("iPhone iOS 17", 2L),
                attribute("ANDROID", 3L),
                attribute("Mozilla/5.0 (Windows)", 4L),
                attribute("SmartTV", 1L),
                attribute(null, 2L)
        ));

        WatchHistoryDto.VideoWatchStatsResponse response =
                watchHistoryService.getVideoWatchStats(STORE_ID);

        assertThat(response.getTotalViews()).isEqualTo(15L);
        assertThat(response.getCompletionRate()).isEqualTo(0.4);
        assertThat(response.getQualityDistribution().getQuality1080p()).isEqualTo(2L);
        assertThat(response.getQualityDistribution().getQuality720p()).isEqualTo(3L);
        assertThat(response.getQualityDistribution().getQuality360p()).isEqualTo(4L);
        assertThat(response.getQualityDistribution().getQualityAuto()).isEqualTo(6L);
        assertThat(response.getDeviceDistribution().getIos()).isEqualTo(2L);
        assertThat(response.getDeviceDistribution().getAndroid()).isEqualTo(3L);
        assertThat(response.getDeviceDistribution().getWeb()).isEqualTo(4L);
        assertThat(response.getDeviceDistribution().getOther()).isEqualTo(3L);
    }

    private WatchHistoryDto.UpdateProgressRequest progressRequest() {
        WatchHistoryDto.UpdateProgressRequest request = new WatchHistoryDto.UpdateProgressRequest();
        request.setSessionId(SESSION_ID);
        request.setDurationWatched(15);
        request.setVideoQuality("720p");
        return request;
    }

    private WatchHistoryDto.CompleteWatchRequest completeRequest() {
        WatchHistoryDto.CompleteWatchRequest request = new WatchHistoryDto.CompleteWatchRequest();
        request.setSessionId(SESSION_ID);
        request.setDurationWatched(30);
        request.setCompletionStatus(true);
        return request;
    }

    private Fp305WatchHistoryRepository.AttributeCount attribute(String value, Long total) {
        return new Fp305WatchHistoryRepository.AttributeCount() {
            @Override
            public String getValue() {
                return value;
            }

            @Override
            public Long getTotal() {
                return total;
            }
        };
    }
}
