package com.plateapp.plate_main.recommendation.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plateapp.plate_main.recommendation.entity.Fp371UserVideoPreference;
import com.plateapp.plate_main.recommendation.entity.Fp372VideoFeature;
import com.plateapp.plate_main.recommendation.entity.Fp373HomeRecommendationCandidate;
import com.plateapp.plate_main.recommendation.entity.Fp374RecommendationServing;
import com.plateapp.plate_main.recommendation.entity.Fp375RecommendationServingItem;
import com.plateapp.plate_main.recommendation.repository.Fp370VideoEventRepository;
import com.plateapp.plate_main.recommendation.repository.Fp371UserVideoPreferenceRepository;
import com.plateapp.plate_main.recommendation.repository.Fp373HomeRecommendationCandidateRepository;
import com.plateapp.plate_main.recommendation.repository.Fp374RecommendationServingRepository;
import com.plateapp.plate_main.recommendation.repository.Fp375RecommendationServingItemRepository;
import com.plateapp.plate_main.user.repository.MemberRepository;
import com.plateapp.plate_main.video.dto.HomeVideoThumbnailDTO;
import com.plateapp.plate_main.video.entity.Fp300Store;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HomeVideoRecommendationService {

    public static final String ALGORITHM_VERSION = "home-video-v1";

    private static final List<String> WATCH_EVENTS = List.of(
            "CLICK",
            "PLAY_START",
            "PLAY_PROGRESS",
            "PLAY_COMPLETE"
    );
    private static final List<String> NEGATIVE_EVENTS = List.of(
            "HIDE",
            "NOT_INTERESTED",
            "REPORT"
    );

    private final Fp370VideoEventRepository eventRepository;
    private final Fp371UserVideoPreferenceRepository preferenceRepository;
    private final Fp373HomeRecommendationCandidateRepository candidateRepository;
    private final Fp374RecommendationServingRepository servingRepository;
    private final Fp375RecommendationServingItemRepository servingItemRepository;
    private final MemberRepository memberRepository;
    private final VideoRecommendationFeatureService featureService;

    public String newRequestId() {
        return "home-video-" + UUID.randomUUID();
    }

    @Transactional
    public RecommendationContext buildContext(
            List<Fp300Store> stores,
            String username,
            boolean isGuest,
            String guestId
    ) {
        RecommendationActor actor = resolveActor(username, isGuest, guestId, null);
        if (stores == null || stores.isEmpty()) {
            return RecommendationContext.empty(actor);
        }

        Map<Integer, Fp372VideoFeature> featureMap = featureService.refreshFeatures(stores);
        Set<Integer> suppressedStoreIds = loadSuppressedStoreIds(actor);
        Map<String, BigDecimal> preferenceScoreMap = loadPreferenceScoreMap(actor, stores, featureMap);

        return new RecommendationContext(actor, featureMap, suppressedStoreIds, preferenceScoreMap);
    }

    public boolean isSuppressed(Fp300Store store, RecommendationContext context) {
        return store != null
                && store.getStoreId() != null
                && context.suppressedStoreIds().contains(store.getStoreId());
    }

    public double personalizationScore(Fp300Store store, RecommendationContext context) {
        if (store == null || context == null || !context.actor().present()) {
            return 0.0;
        }

        Fp372VideoFeature feature = context.featureMap().get(store.getStoreId());
        double score = 0.0;
        score += preferenceScore(context, "PLACE", store.getPlaceId()) * 0.85;
        score += preferenceScore(context, "CREATOR", store.getUsername()) * 0.65;

        if (feature != null) {
            score += preferenceScore(context, "REGION", feature.getRegion1()) * 0.45;
            score += preferenceScore(context, "REGION", feature.getRegion2()) * 0.45;
            score += preferenceScore(context, "DURATION_BUCKET", feature.getDurationBucket()) * 0.25;
            score += doubleValue(feature.getPopularityScore()) * 0.08;
            score += doubleValue(feature.getQualityScore()) * 0.35;
            score += doubleValue(feature.getFreshnessScore()) * 0.08;
        }

        return clamp(score, -6.0, 8.0);
    }

    @Transactional
    public void logHomeServing(
            String requestId,
            String username,
            boolean isGuest,
            String guestId,
            int page,
            int size,
            String sortType,
            Double lat,
            Double lng,
            Double radius,
            Collection<String> placeIds,
            int candidateCount,
            List<HomeVideoThumbnailDTO> servedItems,
            int offset
    ) {
        RecommendationActor actor = resolveActor(username, isGuest, guestId, null);
        if (!actor.present() || servedItems == null) {
            return;
        }

        Fp374RecommendationServing serving = new Fp374RecommendationServing();
        serving.setRequestId(requestId);
        serving.setUserId(actor.userId());
        serving.setUsername(actor.username());
        serving.setIsGuest(actor.guest());
        serving.setGuestId(actor.guestId());
        serving.setSessionId(actor.sessionId());
        serving.setPageNo(page);
        serving.setPageSize(size);
        serving.setSortType(sortType);
        serving.setLatitude(toBigDecimal(lat));
        serving.setLongitude(toBigDecimal(lng));
        serving.setRadiusMeters(toBigDecimal(radius));
        serving.setPlaceIds(toJsonArray(placeIds));
        serving.setAlgorithmVersion(ALGORITHM_VERSION);
        serving.setCandidateCount(candidateCount);
        serving.setServedCount(servedItems.size());
        serving.setFallbackUsed(false);
        Fp374RecommendationServing savedServing = servingRepository.save(serving);

        List<Fp375RecommendationServingItem> itemLogs = new ArrayList<>();
        List<Fp373HomeRecommendationCandidate> candidateLogs = new ArrayList<>();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(30);
        for (int i = 0; i < servedItems.size(); i++) {
            HomeVideoThumbnailDTO item = servedItems.get(i);
            if (item.getStoreId() == null) {
                continue;
            }
            int position = offset + i;

            Fp375RecommendationServingItem itemLog = new Fp375RecommendationServingItem();
            itemLog.setServingId(savedServing.getServingId());
            itemLog.setRequestId(requestId);
            itemLog.setStoreId(item.getStoreId());
            itemLog.setPositionNo(position);
            itemLog.setCandidateSource("PERSONALIZED");
            itemLog.setReasonCode("HOME_PERSONALIZED_RANK");
            itemLogs.add(itemLog);

            Fp373HomeRecommendationCandidate candidate = new Fp373HomeRecommendationCandidate();
            candidate.setBatchKey(requestId);
            candidate.setUserId(actor.userId());
            candidate.setUsername(actor.username());
            candidate.setIsGuest(actor.guest());
            candidate.setGuestId(actor.guestId());
            candidate.setStoreId(item.getStoreId());
            candidate.setCandidateSource("PERSONALIZED");
            candidate.setAlgorithmVersion(ALGORITHM_VERSION);
            candidate.setReasonCode("SERVED");
            candidate.setExpiresAt(expiresAt);
            candidateLogs.add(candidate);
        }

        if (!itemLogs.isEmpty()) {
            servingItemRepository.saveAll(itemLogs);
        }
        if (!candidateLogs.isEmpty()) {
            candidateRepository.saveAll(candidateLogs);
        }
    }

    private RecommendationActor resolveActor(String username, boolean isGuest, String guestId, String sessionId) {
        if (isGuest && hasText(guestId)) {
            return new RecommendationActor(null, null, true, guestId, sessionId);
        }
        if (hasText(username)) {
            Integer userId = memberRepository.findById(username)
                    .map(user -> user.getUserId())
                    .orElse(null);
            return new RecommendationActor(userId, username, false, null, sessionId);
        }
        return RecommendationActor.none();
    }

    private Set<Integer> loadSuppressedStoreIds(RecommendationActor actor) {
        if (!actor.present()) {
            return Collections.emptySet();
        }

        Set<Integer> storeIds = new HashSet<>();
        LocalDateTime recentSince = LocalDateTime.now().minusDays(1);
        LocalDateTime negativeSince = LocalDateTime.now().minusDays(365);

        if (hasText(actor.username())) {
            storeIds.addAll(eventRepository.findRecentStoreIdsByUsername(actor.username(), WATCH_EVENTS, recentSince));
            storeIds.addAll(eventRepository.findRecentStoreIdsByUsername(actor.username(), NEGATIVE_EVENTS, negativeSince));
        } else if (hasText(actor.guestId())) {
            storeIds.addAll(eventRepository.findRecentStoreIdsByGuestId(actor.guestId(), WATCH_EVENTS, recentSince));
            storeIds.addAll(eventRepository.findRecentStoreIdsByGuestId(actor.guestId(), NEGATIVE_EVENTS, negativeSince));
        }
        return storeIds;
    }

    private Map<String, BigDecimal> loadPreferenceScoreMap(
            RecommendationActor actor,
            List<Fp300Store> stores,
            Map<Integer, Fp372VideoFeature> featureMap
    ) {
        if (!actor.present()) {
            return Collections.emptyMap();
        }

        Set<String> subjectTypes = Set.of("PLACE", "CREATOR", "REGION", "DURATION_BUCKET");
        Set<String> subjectKeys = new HashSet<>();
        for (Fp300Store store : stores) {
            add(subjectKeys, store.getPlaceId());
            add(subjectKeys, store.getUsername());
            Fp372VideoFeature feature = featureMap.get(store.getStoreId());
            if (feature != null) {
                add(subjectKeys, feature.getRegion1());
                add(subjectKeys, feature.getRegion2());
                add(subjectKeys, feature.getDurationBucket());
            }
        }

        if (subjectKeys.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Fp371UserVideoPreference> preferences;
        if (hasText(actor.username())) {
            preferences = preferenceRepository.findMatchingUsernamePreferences(
                    actor.username(),
                    UserVideoPreferenceService.MODEL_VERSION,
                    subjectTypes,
                    subjectKeys
            );
        } else {
            preferences = preferenceRepository.findMatchingGuestPreferences(
                    actor.guestId(),
                    UserVideoPreferenceService.MODEL_VERSION,
                    subjectTypes,
                    subjectKeys
            );
        }

        return preferences.stream()
                .filter(p -> hasText(p.getSubjectType()) && hasText(p.getSubjectKey()))
                .collect(Collectors.toMap(
                        p -> key(p.getSubjectType(), p.getSubjectKey()),
                        Fp371UserVideoPreference::getScore,
                        BigDecimal::add,
                        HashMap::new
                ));
    }

    private double preferenceScore(RecommendationContext context, String subjectType, String subjectKey) {
        if (!hasText(subjectKey)) {
            return 0.0;
        }
        return doubleValue(context.preferenceScoreMap().get(key(subjectType, subjectKey)));
    }

    private String key(String subjectType, String subjectKey) {
        return subjectType + ":" + subjectKey;
    }

    private void add(Set<String> values, String value) {
        if (hasText(value)) {
            values.add(value);
        }
    }

    private double doubleValue(BigDecimal value) {
        return value == null ? 0.0 : value.doubleValue();
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private BigDecimal toBigDecimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }

    private String toJsonArray(Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(value -> "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"")
                .collect(Collectors.joining(",", "[", "]"));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record RecommendationContext(
            RecommendationActor actor,
            Map<Integer, Fp372VideoFeature> featureMap,
            Set<Integer> suppressedStoreIds,
            Map<String, BigDecimal> preferenceScoreMap
    ) {
        public static RecommendationContext empty(RecommendationActor actor) {
            return new RecommendationContext(
                    actor,
                    Collections.emptyMap(),
                    Collections.emptySet(),
                    Collections.emptyMap()
            );
        }
    }
}
