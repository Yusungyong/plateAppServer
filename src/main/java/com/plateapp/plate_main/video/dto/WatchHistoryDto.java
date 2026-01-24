package com.plateapp.plate_main.video.dto;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class WatchHistoryDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StartWatchRequest {
        private String deviceInfo;
        private String videoQuality;
        private String sessionId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateProgressRequest {
        private String sessionId;
        private Integer durationWatched;
        private String videoQuality;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CompleteWatchRequest {
        private String sessionId;
        private Integer durationWatched;
        private Boolean completionStatus;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StartWatchResponse {
        private Integer watchId;
        private String sessionId;
        private Integer storeId;
        private OffsetDateTime startedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateProgressResponse {
        private Integer watchId;
        private Integer durationWatched;
        private Double completionRate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CompleteWatchResponse {
        private Integer watchId;
        private Boolean completed;
        private Integer durationWatched;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WatchHistoryItemResponse {
        private Integer watchId;
        private Integer storeId;
        private String storeName;
        private String thumbnail;
        private Integer durationWatched;
        private Integer videoDuration;
        private Double completionRate;
        private Boolean completed;
        private OffsetDateTime watchedAt;
        private String videoQuality;
        private String deviceInfo;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VideoWatchInfoResponse {
        private Boolean hasWatched;
        private OffsetDateTime lastWatchedAt;
        private Integer durationWatched;
        private Integer videoDuration;
        private Double completionRate;
        private Boolean completed;
        private Boolean canResume;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VideoWatchStatsResponse {
        private Integer storeId;
        private Long totalViews;
        private Long uniqueViewers;
        private Double averageDuration;
        private Double completionRate;
        private Long completedViews;
        private QualityDistribution qualityDistribution;
        private DeviceDistribution deviceDistribution;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QualityDistribution {
        private Long quality1080p;
        private Long quality720p;
        private Long quality360p;
        private Long qualityAuto;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DeviceDistribution {
        private Long ios;
        private Long android;
        private Long web;
        private Long other;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PageResponse<T> {
        private Integer page;
        private Integer size;
        private Long total;
        private List<T> items;
    }
}
