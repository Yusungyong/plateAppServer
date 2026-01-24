package com.plateapp.plate_main.video.service;

import com.plateapp.plate_main.user.repository.MemberRepository;
import com.plateapp.plate_main.video.dto.WatchHistoryDto;
import com.plateapp.plate_main.video.entity.Fp300Store;
import com.plateapp.plate_main.video.entity.Fp305WatchHistory;
import com.plateapp.plate_main.video.repository.Fp300StoreRepository;
import com.plateapp.plate_main.video.repository.Fp305WatchHistoryRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WatchHistoryService {

    private static final String USE_Y = "Y";

    private final Fp305WatchHistoryRepository watchHistoryRepository;
    private final Fp300StoreRepository storeRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public WatchHistoryDto.StartWatchResponse startWatch(
            String username,
            Integer storeId,
            WatchHistoryDto.StartWatchRequest request
    ) {
        Fp300Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Store not found: " + storeId));

        Integer userId = memberRepository.findById(username)
                .map(u -> u.getUserId())
                .orElse(null);

        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }

        Fp305WatchHistory history = Fp305WatchHistory.builder()
                .username(username)
                .storeId(storeId)
                .userId(userId)
                .sessionId(sessionId)
                .deviceInfo(request.getDeviceInfo())
                .videoQuality(request.getVideoQuality())
                .durationWatched(0)
                .completionStatus(false)
                .useYn(USE_Y)
                .build();

        Fp305WatchHistory saved = watchHistoryRepository.save(history);

        return WatchHistoryDto.StartWatchResponse.builder()
                .watchId(saved.getId())
                .sessionId(saved.getSessionId())
                .storeId(saved.getStoreId())
                .startedAt(saved.getTimestamp())
                .build();
    }

    @Transactional
    public WatchHistoryDto.UpdateProgressResponse updateProgress(
            Integer storeId,
            WatchHistoryDto.UpdateProgressRequest request
    ) {
        if (request.getSessionId() == null || request.getSessionId().isBlank()) {
            throw new IllegalArgumentException("sessionId is required");
        }

        Fp305WatchHistory history = watchHistoryRepository
                .findBySessionIdAndUseYnAndDeletedAtIsNull(request.getSessionId(), USE_Y)
                .orElseThrow(() -> new IllegalArgumentException("Active watch session not found"));

        int sanitizedDuration = sanitizeDuration(request.getDurationWatched());
        Integer videoDuration = getVideoDuration(storeId);
        int clampedDuration = clampToVideoDuration(sanitizedDuration, videoDuration);

        history.setDurationWatched(clampedDuration);
        history.setVideoQuality(request.getVideoQuality());
        watchHistoryRepository.save(history);

        Double completionRate = calculateCompletionRate(clampedDuration, videoDuration);

        return WatchHistoryDto.UpdateProgressResponse.builder()
                .watchId(history.getId())
                .durationWatched(clampedDuration)
                .completionRate(completionRate)
                .build();
    }

    @Transactional
    public WatchHistoryDto.CompleteWatchResponse completeWatch(
            Integer storeId,
            WatchHistoryDto.CompleteWatchRequest request
    ) {
        if (request.getSessionId() == null || request.getSessionId().isBlank()) {
            throw new IllegalArgumentException("sessionId is required");
        }

        Fp305WatchHistory history = watchHistoryRepository
                .findBySessionIdAndUseYnAndDeletedAtIsNull(request.getSessionId(), USE_Y)
                .orElseThrow(() -> new IllegalArgumentException("Active watch session not found"));

        int sanitizedDuration = sanitizeDuration(request.getDurationWatched());
        Integer videoDuration = getVideoDuration(storeId);
        int clampedDuration = clampToVideoDuration(sanitizedDuration, videoDuration);

        history.setCompletionStatus(Boolean.TRUE.equals(request.getCompletionStatus()));
        history.setDurationWatched(clampedDuration);
        watchHistoryRepository.save(history);

        return WatchHistoryDto.CompleteWatchResponse.builder()
                .watchId(history.getId())
                .completed(Boolean.TRUE.equals(history.getCompletionStatus()))
                .durationWatched(clampedDuration)
                .build();
    }

    @Transactional(readOnly = true)
    public WatchHistoryDto.PageResponse<WatchHistoryDto.WatchHistoryItemResponse> getWatchHistory(
            String username,
            boolean completedOnly,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Fp305WatchHistory> historyPage = watchHistoryRepository
                .findByUsernameOrderByTimestampDesc(username, completedOnly, pageable);

        List<WatchHistoryDto.WatchHistoryItemResponse> items = historyPage.getContent().stream()
                .map(this::toHistoryItemResponse)
                .collect(Collectors.toList());

        return WatchHistoryDto.PageResponse.<WatchHistoryDto.WatchHistoryItemResponse>builder()
                .page(page)
                .size(size)
                .total(historyPage.getTotalElements())
                .items(items)
                .build();
    }

    @Transactional(readOnly = true)
    public WatchHistoryDto.VideoWatchInfoResponse getVideoWatchInfo(
            String username,
            Integer storeId
    ) {
        Fp305WatchHistory latestWatch = watchHistoryRepository
                .findFirstByUsernameAndStoreIdAndUseYnAndDeletedAtIsNullOrderByTimestampDesc(
                        username, storeId, USE_Y)
                .orElse(null);

        if (latestWatch == null) {
            return WatchHistoryDto.VideoWatchInfoResponse.builder()
                    .hasWatched(false)
                    .canResume(false)
                    .build();
        }

        Integer videoDuration = getVideoDuration(storeId);
        int durationWatched = sanitizeDuration(latestWatch.getDurationWatched());
        Double completionRate = calculateCompletionRate(durationWatched, videoDuration);

        boolean completed = Boolean.TRUE.equals(latestWatch.getCompletionStatus());
        boolean canResume = !completed && completionRate >= 0.1;

        return WatchHistoryDto.VideoWatchInfoResponse.builder()
                .hasWatched(true)
                .lastWatchedAt(latestWatch.getTimestamp())
                .durationWatched(durationWatched)
                .videoDuration(videoDuration)
                .completionRate(completionRate)
                .completed(completed)
                .canResume(canResume)
                .build();
    }

    @Transactional(readOnly = true)
    public WatchHistoryDto.VideoWatchStatsResponse getVideoWatchStats(Integer storeId) {
        Long totalViews = watchHistoryRepository.countByStoreIdAndUseYnAndDeletedAtIsNull(storeId, USE_Y);
        Long uniqueViewers = watchHistoryRepository.countUniqueViewersByStoreId(storeId);
        Long completedViews = watchHistoryRepository.countByStoreIdAndCompletionStatusAndUseYnAndDeletedAtIsNull(
                storeId, true, USE_Y);
        Double averageDuration = watchHistoryRepository.getAverageDurationByStoreId(storeId);

        Double completionRate = 0.0;
        if (totalViews > 0) {
            completionRate = (double) completedViews / totalViews;
        }

        WatchHistoryDto.QualityDistribution qualityDist = WatchHistoryDto.QualityDistribution.builder()
                .quality1080p(0L)
                .quality720p(0L)
                .quality360p(0L)
                .qualityAuto(0L)
                .build();

        WatchHistoryDto.DeviceDistribution deviceDist = WatchHistoryDto.DeviceDistribution.builder()
                .ios(0L)
                .android(0L)
                .web(0L)
                .other(0L)
                .build();

        return WatchHistoryDto.VideoWatchStatsResponse.builder()
                .storeId(storeId)
                .totalViews(totalViews)
                .uniqueViewers(uniqueViewers)
                .averageDuration(averageDuration != null ? averageDuration : 0.0)
                .completionRate(completionRate)
                .completedViews(completedViews)
                .qualityDistribution(qualityDist)
                .deviceDistribution(deviceDist)
                .build();
    }

    private Integer getVideoDuration(Integer storeId) {
        Fp300Store store = storeRepository.findById(storeId).orElse(null);
        return store != null ? store.getVideoDuration() : null;
    }

    private int sanitizeDuration(Integer durationWatched) {
        if (durationWatched == null || durationWatched < 0) {
            return 0;
        }
        return durationWatched;
    }

    private int clampToVideoDuration(int durationWatched, Integer videoDuration) {
        if (videoDuration == null || videoDuration <= 0) {
            return durationWatched;
        }
        return Math.min(durationWatched, videoDuration);
    }

    private double calculateCompletionRate(int durationWatched, Integer videoDuration) {
        if (videoDuration == null || videoDuration <= 0) {
            return 0.0;
        }
        double rate = (double) durationWatched / videoDuration;
        if (rate < 0.0) {
            return 0.0;
        }
        if (rate > 1.0) {
            return 1.0;
        }
        return rate;
    }

    private WatchHistoryDto.WatchHistoryItemResponse toHistoryItemResponse(Fp305WatchHistory history) {
        Fp300Store store = storeRepository.findById(history.getStoreId()).orElse(null);

        String storeName = null;
        String thumbnail = null;
        Integer videoDuration = null;

        if (store != null) {
            storeName = store.getStoreName();
            thumbnail = store.getThumbnail();
            videoDuration = store.getVideoDuration();
        }

        Integer durationWatched = sanitizeDuration(history.getDurationWatched());
        Double completionRate = calculateCompletionRate(durationWatched, videoDuration);
        boolean completed = Boolean.TRUE.equals(history.getCompletionStatus());

        return WatchHistoryDto.WatchHistoryItemResponse.builder()
                .watchId(history.getId())
                .storeId(history.getStoreId())
                .storeName(storeName)
                .thumbnail(thumbnail)
                .durationWatched(durationWatched)
                .videoDuration(videoDuration)
                .completionRate(completionRate)
                .completed(completed)
                .watchedAt(history.getTimestamp())
                .videoQuality(history.getVideoQuality())
                .deviceInfo(history.getDeviceInfo())
                .build();
    }
}
