package com.plateapp.plate_main.home.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import com.plateapp.plate_main.home.dto.HomeImpressionRequest;
import com.plateapp.plate_main.home.dto.HomeImpressionResponse;
import com.plateapp.plate_main.home.entity.Fp376HomeImpression;
import com.plateapp.plate_main.home.repository.Fp376HomeImpressionRepository;
import com.plateapp.plate_main.recommendation.service.RecommendationActor;
import com.plateapp.plate_main.recommendation.service.RecommendationActorResolver;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HomeImpressionService {

    private static final String TYPE_VIDEO = "VIDEO";
    private static final String TYPE_IMAGE = "IMAGE";
    private static final String SURFACE_DEFAULT = "home";
    private static final Duration SUPPRESSION_WINDOW = Duration.ofHours(48);

    private final Fp376HomeImpressionRepository impressionRepository;
    private final RecommendationActorResolver actorResolver;

    @Transactional
    public HomeImpressionResponse record(HomeImpressionRequest request, Authentication authentication) {
        RecommendationActor actor = actorResolver.resolve(
                authentication,
                null,
                Boolean.TRUE.equals(request.isGuest()),
                request.guestId(),
                request.sessionId()
        );
        if (!actor.present()) {
            throw new AppException(ErrorCode.COMMON_MISSING_PARAMETER, "username or guestId is required");
        }

        Map<String, Fp376HomeImpression> uniqueRows = new LinkedHashMap<>();
        for (HomeImpressionRequest.Item item : request.items()) {
            Fp376HomeImpression row = toEntity(request, item, actor);
            uniqueRows.putIfAbsent(uniqueKey(row), row);
        }

        List<Fp376HomeImpression> saved = uniqueRows.isEmpty()
                ? List.of()
                : impressionRepository.saveAll(new ArrayList<>(uniqueRows.values()));

        return new HomeImpressionResponse(
                saved.size(),
                Math.max(0, request.items().size() - uniqueRows.size()),
                LocalDateTime.now().plus(SUPPRESSION_WINDOW)
        );
    }

    @Transactional(readOnly = true)
    public HomeImpressionExclusion loadRecentExclusion(String username, boolean isGuest, String guestId) {
        if (hasText(username)) {
            LocalDateTime since = suppressionSince();
            return new HomeImpressionExclusion(
                    Set.copyOf(impressionRepository.findRecentVideoStoreIdsByUsername(username, since)),
                    Set.copyOf(impressionRepository.findRecentImageFeedNosByUsername(username, since))
            );
        }
        if (isGuest && hasText(guestId)) {
            LocalDateTime since = suppressionSince();
            return new HomeImpressionExclusion(
                    Set.copyOf(impressionRepository.findRecentVideoStoreIdsByGuestId(guestId, since)),
                    Set.copyOf(impressionRepository.findRecentImageFeedNosByGuestId(guestId, since))
            );
        }
        return HomeImpressionExclusion.empty();
    }

    @Transactional(readOnly = true)
    public Set<Integer> findRecentVideoStoreIds(String username, boolean isGuest, String guestId) {
        LocalDateTime since = suppressionSince();
        if (hasText(username)) {
            return Set.copyOf(impressionRepository.findRecentVideoStoreIdsByUsername(username, since));
        }
        if (isGuest && hasText(guestId)) {
            return Set.copyOf(impressionRepository.findRecentVideoStoreIdsByGuestId(guestId, since));
        }
        return Set.of();
    }

    @Transactional(readOnly = true)
    public Set<Integer> findRecentImageFeedNos(String username, boolean isGuest, String guestId) {
        LocalDateTime since = suppressionSince();
        if (hasText(username)) {
            return Set.copyOf(impressionRepository.findRecentImageFeedNosByUsername(username, since));
        }
        if (isGuest && hasText(guestId)) {
            return Set.copyOf(impressionRepository.findRecentImageFeedNosByGuestId(guestId, since));
        }
        return Set.of();
    }

    public LocalDateTime suppressionSince() {
        return LocalDateTime.now().minus(SUPPRESSION_WINDOW);
    }

    private Fp376HomeImpression toEntity(
            HomeImpressionRequest request,
            HomeImpressionRequest.Item item,
            RecommendationActor actor
    ) {
        String contentType = normalizeContentType(item.contentType());
        validateItem(contentType, item);

        Fp376HomeImpression row = new Fp376HomeImpression();
        row.setUserId(actor.userId());
        row.setUsername(actor.username());
        row.setIsGuest(actor.guest());
        row.setGuestId(actor.guestId());
        row.setSessionId(actor.sessionId());
        row.setDeviceId(blankToNull(request.deviceId()));
        row.setContentType(contentType);
        row.setStoreId(TYPE_VIDEO.equals(contentType) ? item.storeId() : null);
        row.setFeedNo(TYPE_IMAGE.equals(contentType) ? item.feedNo() : null);
        row.setSurface(defaultText(request.surface(), SURFACE_DEFAULT));
        row.setRequestId(blankToNull(request.requestId()));
        row.setPositionNo(item.positionNo());
        row.setClientImpressedAt(item.clientImpressedAt());
        return row;
    }

    private void validateItem(String contentType, HomeImpressionRequest.Item item) {
        if (TYPE_VIDEO.equals(contentType) && item.storeId() == null) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "VIDEO impression requires storeId");
        }
        if (TYPE_IMAGE.equals(contentType) && item.feedNo() == null) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "IMAGE impression requires feedNo");
        }
    }

    private String normalizeContentType(String value) {
        String normalized = value == null ? null : value.trim().toUpperCase(Locale.ROOT);
        if (!TYPE_VIDEO.equals(normalized) && !TYPE_IMAGE.equals(normalized)) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "contentType must be VIDEO or IMAGE");
        }
        return normalized;
    }

    private String uniqueKey(Fp376HomeImpression row) {
        String actorKey = hasText(row.getUsername()) ? "u:" + row.getUsername() : "g:" + row.getGuestId();
        String contentKey = TYPE_VIDEO.equals(row.getContentType())
                ? "v:" + row.getStoreId()
                : "i:" + row.getFeedNo();
        String requestKey = row.getRequestId() == null ? "" : row.getRequestId();
        return actorKey + "|" + requestKey + "|" + row.getSurface() + "|" + contentKey;
    }

    private String defaultText(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }

    private String blankToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
