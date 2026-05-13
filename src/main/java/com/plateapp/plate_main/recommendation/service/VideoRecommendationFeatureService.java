package com.plateapp.plate_main.recommendation.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plateapp.plate_main.like.service.LikeService;
import com.plateapp.plate_main.recommendation.entity.Fp372VideoFeature;
import com.plateapp.plate_main.recommendation.repository.Fp370VideoEventRepository;
import com.plateapp.plate_main.recommendation.repository.Fp372VideoFeatureRepository;
import com.plateapp.plate_main.video.entity.Fp300Store;
import com.plateapp.plate_main.video.entity.Fp310Place;
import com.plateapp.plate_main.video.repository.Fp310PlaceRepository;
import com.plateapp.plate_main.video.repository.Fp440CommentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VideoRecommendationFeatureService {

    private static final String FLAG_Y = "Y";

    private final Fp372VideoFeatureRepository featureRepository;
    private final Fp310PlaceRepository placeRepository;
    private final Fp440CommentRepository commentRepository;
    private final Fp370VideoEventRepository eventRepository;
    private final LikeService likeService;

    @Transactional
    public Map<Integer, Fp372VideoFeature> refreshFeatures(List<Fp300Store> stores) {
        if (stores == null || stores.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Integer> storeIds = stores.stream()
                .map(Fp300Store::getStoreId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (storeIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Integer, Fp372VideoFeature> existing = featureRepository.findByStoreIdIn(storeIds).stream()
                .collect(Collectors.toMap(Fp372VideoFeature::getStoreId, Function.identity()));
        Map<String, Fp310Place> placeMap = loadPlaceMap(stores);
        Map<Integer, Long> likeCounts = defaultMap(likeService.getLikeCountMap(storeIds));
        Map<Integer, Long> commentCounts = loadCommentCounts(storeIds);
        Map<Integer, Map<String, Long>> eventCounts = loadEventCounts(storeIds);

        List<Fp372VideoFeature> features = stores.stream()
                .filter(store -> store.getStoreId() != null)
                .map(store -> updateFeature(
                        existing.getOrDefault(store.getStoreId(), new Fp372VideoFeature()),
                        store,
                        placeMap.get(store.getPlaceId()),
                        likeCounts.getOrDefault(store.getStoreId(), 0L),
                        commentCounts.getOrDefault(store.getStoreId(), 0L),
                        eventCounts.getOrDefault(store.getStoreId(), Collections.emptyMap())
                ))
                .toList();

        return featureRepository.saveAll(features).stream()
                .collect(Collectors.toMap(Fp372VideoFeature::getStoreId, Function.identity()));
    }

    private Fp372VideoFeature updateFeature(
            Fp372VideoFeature feature,
            Fp300Store store,
            Fp310Place place,
            long likeCount,
            long commentCount,
            Map<String, Long> eventCounts
    ) {
        feature.setStoreId(store.getStoreId());
        feature.setPlaceId(store.getPlaceId());
        feature.setCreatorUsername(store.getUsername());
        feature.setStoreName(store.getStoreName());
        feature.setTitle(store.getTitle());
        feature.setAddress(store.getAddress());
        feature.setVideoDuration(store.getVideoDuration());
        feature.setDurationBucket(durationBucket(store.getVideoDuration()));
        feature.setContentCreatedAt(store.getCreatedAt());
        feature.setContentUpdatedAt(store.getUpdatedAt());

        if (place != null) {
            feature.setLatitude(toBigDecimal(place.getLatitude()));
            feature.setLongitude(toBigDecimal(place.getLongitude()));
            feature.setRegion1(place.getAdministrativeAreaLevel1());
            feature.setRegion2(place.getAdministrativeAreaLevel2() != null
                    ? place.getAdministrativeAreaLevel2()
                    : place.getLocality());
        }

        long impressions = eventCounts.getOrDefault("IMPRESSION", 0L);
        long clicks = eventCounts.getOrDefault("CLICK", 0L);
        long plays = eventCounts.getOrDefault("PLAY_START", 0L);
        long completes = eventCounts.getOrDefault("PLAY_COMPLETE", 0L);
        long hides = eventCounts.getOrDefault("HIDE", 0L) + eventCounts.getOrDefault("NOT_INTERESTED", 0L);
        long reports = eventCounts.getOrDefault("REPORT", 0L);

        feature.setLikeCount(likeCount);
        feature.setCommentCount(commentCount);
        feature.setImpressionCount(impressions);
        feature.setClickCount(clicks);
        feature.setPlayCount(plays);
        feature.setCompleteCount(completes);
        feature.setHideCount(hides);
        feature.setReportCount(reports);
        feature.setPopularityScore(score(likeCount * 0.35 + commentCount * 0.25 + plays * 0.08 + completes * 0.18));
        feature.setQualityScore(score(qualityScore(impressions, clicks, plays, completes, hides, reports)));
        feature.setFreshnessScore(score(freshnessScore(store)));
        feature.setFeatureRefreshedAt(LocalDateTime.now());
        return feature;
    }

    private Map<String, Fp310Place> loadPlaceMap(List<Fp300Store> stores) {
        List<String> placeIds = stores.stream()
                .map(Fp300Store::getPlaceId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (placeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return placeRepository.findByPlaceIdInAndUseYnAndDeletedAtIsNull(placeIds, FLAG_Y).stream()
                .filter(place -> place.getPlaceId() != null)
                .collect(Collectors.toMap(Fp310Place::getPlaceId, Function.identity(), (left, right) -> left));
    }

    private Map<Integer, Long> loadCommentCounts(Collection<Integer> storeIds) {
        if (storeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return commentRepository.countActiveByStoreIds(storeIds).stream()
                .collect(Collectors.toMap(
                        Fp440CommentRepository.StoreCommentCount::getStoreId,
                        Fp440CommentRepository.StoreCommentCount::getCnt
                ));
    }

    private Map<Integer, Map<String, Long>> loadEventCounts(Collection<Integer> storeIds) {
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        Map<Integer, Map<String, Long>> result = new HashMap<>();
        for (Fp370VideoEventRepository.StoreEventCount row : eventRepository.countByStoreIdsSince(storeIds, since)) {
            result.computeIfAbsent(row.getStoreId(), ignored -> new HashMap<>())
                    .put(row.getEventType(), row.getCnt());
        }
        return result;
    }

    private String durationBucket(Integer seconds) {
        if (seconds == null) {
            return null;
        }
        if (seconds <= 30) {
            return "SHORT";
        }
        if (seconds <= 90) {
            return "MEDIUM";
        }
        return "LONG";
    }

    private double qualityScore(long impressions, long clicks, long plays, long completes, long hides, long reports) {
        double ctr = impressions > 0 ? (double) clicks / impressions : 0.0;
        double playRate = impressions > 0 ? (double) plays / impressions : 0.0;
        double completeRate = plays > 0 ? (double) completes / plays : 0.0;
        double negativeRate = impressions > 0 ? (double) (hides + reports) / impressions : 0.0;
        return ctr * 3.0 + playRate * 2.0 + completeRate * 3.0 - negativeRate * 5.0;
    }

    private double freshnessScore(Fp300Store store) {
        if (store.getCreatedAt() == null) {
            return 0.0;
        }
        long days = Math.max(0, ChronoUnit.DAYS.between(store.getCreatedAt(), java.time.LocalDate.now()));
        return Math.max(0.0, 5.0 - Math.min(days, 30) * 0.12);
    }

    private BigDecimal score(double value) {
        return BigDecimal.valueOf(value).setScale(6, java.math.RoundingMode.HALF_UP);
    }

    private BigDecimal toBigDecimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }

    private <K, V> Map<K, V> defaultMap(Map<K, V> map) {
        return map != null ? map : Collections.emptyMap();
    }
}
