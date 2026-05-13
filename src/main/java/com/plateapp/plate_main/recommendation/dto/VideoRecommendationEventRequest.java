package com.plateapp.plate_main.recommendation.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VideoRecommendationEventRequest {

    private String eventUid;

    @NotNull
    private Integer storeId;

    @NotBlank
    private String eventType;

    private String eventSource = "HOME";
    private String requestId;
    private String algorithmVersion;
    private String guestId;
    private Boolean isGuest;
    private String sessionId;
    private String deviceId;
    private Integer impressionPosition;
    private Integer playPositionMs;
    private Integer watchDurationMs;
    private Integer videoDurationMs;
    private BigDecimal completionRatio;
    private LocalDateTime clientEventAt;
}
