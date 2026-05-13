package com.plateapp.plate_main.recommendation.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import com.plateapp.plate_main.recommendation.dto.VideoRecommendationEventRequest;
import com.plateapp.plate_main.recommendation.dto.VideoRecommendationEventResponse;
import com.plateapp.plate_main.recommendation.entity.Fp370VideoEvent;
import com.plateapp.plate_main.recommendation.entity.Fp372VideoFeature;
import com.plateapp.plate_main.recommendation.repository.Fp370VideoEventRepository;
import com.plateapp.plate_main.video.entity.Fp300Store;
import com.plateapp.plate_main.video.repository.Fp300StoreRepository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VideoRecommendationEventService {

    private static final List<String> VALID_EVENT_TYPES = List.of(
            "IMPRESSION",
            "CLICK",
            "PLAY_START",
            "PLAY_PROGRESS",
            "PLAY_COMPLETE",
            "SKIP",
            "LIKE",
            "UNLIKE",
            "COMMENT",
            "SHARE",
            "HIDE",
            "NOT_INTERESTED",
            "REPORT"
    );

    private final Fp370VideoEventRepository eventRepository;
    private final Fp300StoreRepository storeRepository;
    private final RecommendationActorResolver actorResolver;
    private final VideoRecommendationFeatureService featureService;
    private final UserVideoPreferenceService preferenceService;

    @Transactional
    public VideoRecommendationEventResponse record(
            VideoRecommendationEventRequest request,
            Authentication authentication,
            HttpServletRequest servletRequest
    ) {
        String eventType = normalize(request.getEventType());
        if (!VALID_EVENT_TYPES.contains(eventType)) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT);
        }

        if (hasText(request.getEventUid()) && eventRepository.existsByEventUid(request.getEventUid())) {
            Long eventId = eventRepository.findByEventUid(request.getEventUid())
                    .map(Fp370VideoEvent::getEventId)
                    .orElse(null);
            return new VideoRecommendationEventResponse(eventId, true);
        }

        RecommendationActor actor = actorResolver.resolve(
                authentication,
                null,
                Boolean.TRUE.equals(request.getIsGuest()),
                request.getGuestId(),
                request.getSessionId()
        );
        if (!actor.present()) {
            throw new AppException(ErrorCode.COMMON_MISSING_PARAMETER);
        }

        Fp300Store store = storeRepository.findByStoreIdAndUseYnAndOpenYnAndDeletedAtIsNull(
                request.getStoreId(),
                "Y",
                "Y"
        ).orElseThrow(() -> new AppException(ErrorCode.COMMON_NOT_FOUND));

        Fp370VideoEvent event = new Fp370VideoEvent();
        event.setEventUid(blankToNull(request.getEventUid()));
        event.setUserId(actor.userId());
        event.setUsername(actor.username());
        event.setIsGuest(actor.guest());
        event.setGuestId(actor.guestId());
        event.setSessionId(actor.sessionId());
        event.setDeviceId(blankToNull(request.getDeviceId()));
        event.setStoreId(store.getStoreId());
        event.setPlaceId(store.getPlaceId());
        event.setCreatorUsername(store.getUsername());
        event.setEventType(eventType);
        event.setEventSource(defaultText(normalize(request.getEventSource()), "HOME"));
        event.setRequestId(blankToNull(request.getRequestId()));
        event.setAlgorithmVersion(blankToNull(request.getAlgorithmVersion()));
        event.setImpressionPosition(request.getImpressionPosition());
        event.setPlayPositionMs(nonNegative(request.getPlayPositionMs()));
        event.setWatchDurationMs(nonNegative(request.getWatchDurationMs()));
        event.setVideoDurationMs(nonNegative(request.getVideoDurationMs()));
        event.setCompletionRatio(normalizeRatio(request.getCompletionRatio()));
        event.setClientEventAt(request.getClientEventAt());
        event.setIpAddress(clientIp(servletRequest));
        event.setUserAgent(servletRequest.getHeader("User-Agent"));

        Fp370VideoEvent saved = eventRepository.save(event);
        Fp372VideoFeature feature = featureService.refreshFeatures(List.of(store)).get(store.getStoreId());
        preferenceService.updateFromEvent(saved, store, feature);

        return new VideoRecommendationEventResponse(saved.getEventId(), false);
    }

    private Integer nonNegative(Integer value) {
        return value == null ? null : Math.max(0, value);
    }

    private BigDecimal normalizeRatio(BigDecimal value) {
        if (value == null) {
            return null;
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        if (value.compareTo(BigDecimal.ONE) > 0) {
            return BigDecimal.ONE;
        }
        return value;
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private String defaultText(String value, String fallback) {
        return hasText(value) ? value : fallback;
    }

    private String blankToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
