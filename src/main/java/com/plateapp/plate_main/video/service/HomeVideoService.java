// src/main/java/com/plateapp/plate_main/video/service/HomeVideoService.java
package com.plateapp.plate_main.video.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashSet;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import com.plateapp.plate_main.common.s3.S3UploadService;
import com.plateapp.plate_main.block.repository.BlockRepository;
import com.plateapp.plate_main.home.service.HomeImpressionService;
import com.plateapp.plate_main.report.repository.ReportRepository;
import com.plateapp.plate_main.like.service.LikeService;
import com.plateapp.plate_main.recommendation.service.HomeVideoRecommendationService;
import com.plateapp.plate_main.recommendation.service.HomeVideoRecommendationService.RecommendationContext;
import com.plateapp.plate_main.user.entity.Fp100User;
import com.plateapp.plate_main.user.repository.MemberRepository;
import com.plateapp.plate_main.video.dto.HomeVideoThumbnailDTO;
import com.plateapp.plate_main.video.dto.VideoFeedItemDTO;
import com.plateapp.plate_main.video.dto.VideoWatchHistoryCreateRequest;
import com.plateapp.plate_main.video.entity.Fp300Store;
import com.plateapp.plate_main.video.entity.Fp303WatchHistory;
import com.plateapp.plate_main.video.entity.Fp310Place;
import com.plateapp.plate_main.video.service.ContentPlaceResolver.ResolvedPlace;
import com.plateapp.plate_main.video.repository.Fp300StoreRepository;
import com.plateapp.plate_main.video.repository.Fp303WatchHistoryRepository;
import com.plateapp.plate_main.video.repository.Fp310PlaceRepository;
import com.plateapp.plate_main.video.repository.Fp440CommentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HomeVideoService {

    private static final String FLAG_Y = "Y";
    private static final double DEFAULT_RADIUS_METERS = 2000.0;
    private static final double RADIUS_STEP_METERS = 500.0;
    private static final int FEED_TOTAL_LIMIT = 10;
    private static final double NEARBY_RADIUS_DEFAULT = 1500.0;
    private static final double NEARBY_RADIUS_MIN = 300.0;
    private static final double NEARBY_RADIUS_MAX = 5000.0;
    private static final int VIDEO_SIZE_MAX = 50;
    private static final int HOME_CANDIDATE_FETCH_MAX = 120;
    private static final int HOME_CANDIDATE_FETCH_MULTIPLIER = 3;
    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    private final Fp300StoreRepository fp300StoreRepository;
    private final Fp303WatchHistoryRepository fp303WatchHistoryRepository;
    private final Fp310PlaceRepository fp310PlaceRepository;

    private final Fp440CommentRepository fp440CommentRepository;
    private final MemberRepository memberRepository;
    private final BlockRepository blockRepository;
    private final ReportRepository reportRepository;

    private final LikeService likeService;
    private final S3UploadService s3UploadService;
    private final HomeVideoRecommendationService homeVideoRecommendationService;
    private final ContentPlaceResolver contentPlaceResolver;
    private final HomeImpressionService homeImpressionService;

    public Page<HomeVideoThumbnailDTO> getHomeVideoThumbnails(
            int page,
            int size,
            String sortType,
            Double lat,
            Double lng,
            Double radius,
            String username,
            boolean isGuest,
            String guestId,
            List<String> placeIds
    ) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(size, 1), VIDEO_SIZE_MAX);
        Pageable pageable = PageRequest.of(safePage, safeSize);
        Pageable candidatePageable = PageRequest.of(0, resolveCandidateFetchSize(safePage, safeSize));
        Set<String> excluded = loadExcludedUsernames(username);
        String requestId = homeVideoRecommendationService.newRequestId();

        if (isNearby(sortType)) {
            validateNearbyParams(lat, lng);
            double safeRadius = normalizeRadius(radius);
            Page<Fp300Store> entityPage = fp300StoreRepository.findHomeVideoThumbnailsNearby(
                    lat,
                    lng,
                    safeRadius,
                    username,
                    candidatePageable
            );
            return rerankHomeVideoPage(
                    entityPage.getContent(),
                    entityPage.getTotalElements(),
                    pageable,
                    excluded,
                    username,
                    isGuest,
                    guestId,
                    lat,
                    lng,
                    safeRadius,
                    requestId,
                    sortType,
                    placeIds
            );
        }

        boolean usePlaceFilter = placeIds != null && !placeIds.isEmpty();
        List<String> safePlaceIds = usePlaceFilter ? placeIds : Collections.singletonList("DUMMY");

        Page<Fp300Store> entityPage =
                fp300StoreRepository.findHomeVideoThumbnails(
                        username,
                        isGuest,
                        guestId,
                        usePlaceFilter,
                        safePlaceIds,
                        candidatePageable
                );

        return rerankHomeVideoPage(
                entityPage.getContent(),
                entityPage.getTotalElements(),
                pageable,
                excluded,
                username,
                isGuest,
                guestId,
                lat,
                lng,
                null,
                requestId,
                sortType,
                placeIds
        );
    }

    private HomeVideoThumbnailDTO toThumbnailDto(Fp300Store e) {
        return HomeVideoThumbnailDTO.builder()
                .storeId(e.getStoreId())
                .username(e.getUsername())
                .title(e.getTitle())
                .fileName(s3UploadService.toVideoUrl(e.getFileName()))
                .thumbnail(s3UploadService.toImageUrl(e.getThumbnail()))
                .videoDuration(e.getVideoDuration())
                .muteYn(e.getMuteYn())
                .videoSize(e.getVideoSize())
                .storeName(e.getStoreName())
                .address(e.getAddress())
                .placeId(e.getPlaceId())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    private boolean isNearby(String sortType) {
        return sortType != null && "NEARBY".equalsIgnoreCase(sortType);
    }

    private int resolveCandidateFetchSize(int safePage, int safeSize) {
        int requested = (safePage + 1) * safeSize * HOME_CANDIDATE_FETCH_MULTIPLIER;
        return Math.min(Math.max(requested, safeSize), HOME_CANDIDATE_FETCH_MAX);
    }

    private Page<HomeVideoThumbnailDTO> rerankHomeVideoPage(
            List<Fp300Store> candidates,
            long originalTotal,
            Pageable pageable,
            Set<String> excluded,
            String username,
            boolean isGuest,
            String guestId,
            Double lat,
            Double lng,
            Double radiusMeters,
            String requestId,
            String sortType,
            List<String> placeIds
    ) {
        List<Fp300Store> filtered = candidates.stream()
                .filter(store -> excluded.isEmpty() || store.getUsername() == null || !excluded.contains(store.getUsername()))
                .toList();

        if (filtered.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        List<HomeVideoThumbnailDTO> ranked = buildRankedHomeThumbnails(
                filtered,
                username,
                isGuest,
                guestId,
                lat,
                lng,
                radiusMeters,
                requestId
        );
        int offset = (int) pageable.getOffset();
        if (offset >= ranked.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, Math.min(originalTotal, ranked.size()));
        }

        Set<Integer> recentStoreIds = homeImpressionService.findRecentVideoStoreIds(username, isGuest, guestId);
        List<HomeVideoThumbnailDTO> content = sliceRankedVideos(ranked, offset, pageable.getPageSize(), recentStoreIds);
        homeVideoRecommendationService.logHomeServing(
                requestId,
                username,
                isGuest,
                guestId,
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sortType,
                lat,
                lng,
                radiusMeters,
                placeIds,
                ranked.size(),
                content,
                offset
        );
        long total = Math.min(originalTotal, ranked.size());
        return new PageImpl<>(content, pageable, total);
    }

    private List<HomeVideoThumbnailDTO> sliceRankedVideos(
            List<HomeVideoThumbnailDTO> ranked,
            int offset,
            int limit,
            Set<Integer> recentStoreIds
    ) {
        if (ranked == null || ranked.isEmpty() || offset >= ranked.size()) {
            return Collections.emptyList();
        }
        List<HomeVideoThumbnailDTO> content = new ArrayList<>();
        int index = Math.max(0, offset);
        while (index < ranked.size() && content.size() < limit) {
            HomeVideoThumbnailDTO item = ranked.get(index);
            index++;
            Integer storeId = item.getStoreId();
            if (storeId == null || !recentStoreIds.contains(storeId)) {
                content.add(item);
            }
        }
        return content;
    }

    private List<HomeVideoThumbnailDTO> buildRankedHomeThumbnails(
            List<Fp300Store> stores,
            String username,
            boolean isGuest,
            String guestId,
            Double lat,
            Double lng,
            Double radiusMeters,
            String requestId
    ) {
        RecommendationContext recommendationContext =
                homeVideoRecommendationService.buildContext(stores, username, isGuest, guestId);
        List<Fp300Store> personalizedCandidates = stores.stream()
                .filter(store -> !homeVideoRecommendationService.isSuppressed(store, recommendationContext))
                .toList();
        if (personalizedCandidates.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> storeIds = extractStoreIds(personalizedCandidates);
        Map<Integer, Long> likeCountMap = defaultMap(likeService.getLikeCountMap(storeIds));
        Map<Integer, Long> commentCountMap = defaultMap(loadCommentCountMap(storeIds));
        Set<Integer> myLikedStoreIdSet = defaultSet(likeService.getMyLikedStoreIdSet(username, storeIds));
        Map<String, Fp310Place> placeMap = loadPlaceMap(personalizedCandidates);

        List<ScoredHomeVideoCandidate> scored = personalizedCandidates.stream()
                .map(store -> new ScoredHomeVideoCandidate(
                        store,
                        calculateHomeThumbnailScore(store, likeCountMap, commentCountMap, myLikedStoreIdSet, placeMap, lat, lng, radiusMeters)
                                + homeVideoRecommendationService.personalizationScore(store, recommendationContext)
                ))
                .sorted(Comparator.comparingDouble(ScoredHomeVideoCandidate::baseScore).reversed())
                .toList();

        return diversifyRankedCandidates(scored).stream()
                .map(candidate -> {
                    HomeVideoThumbnailDTO dto = toThumbnailDto(candidate.store());
                    dto.setRecommendationRequestId(requestId);
                    return dto;
                })
                .toList();
    }

    private double calculateHomeThumbnailScore(
            Fp300Store store,
            Map<Integer, Long> likeCountMap,
            Map<Integer, Long> commentCountMap,
            Set<Integer> myLikedStoreIdSet,
            Map<String, Fp310Place> placeMap,
            Double lat,
            Double lng,
            Double radiusMeters
    ) {
        Integer storeId = store.getStoreId();
        long likeCount = storeId != null ? likeCountMap.getOrDefault(storeId, 0L) : 0L;
        long commentCount = storeId != null ? commentCountMap.getOrDefault(storeId, 0L) : 0L;

        double score = 0.0;
        score += recencyScore(store);
        score += Math.min(likeCount * 0.35, 4.0);
        score += Math.min(commentCount * 0.25, 3.0);

        if (storeId != null && myLikedStoreIdSet.contains(storeId)) {
            score += 1.4;
        }

        Double distanceMeters = resolveDistanceMeters(store, placeMap, lat, lng);
        if (distanceMeters != null) {
            double effectiveRadius = radiusMeters != null ? radiusMeters : NEARBY_RADIUS_DEFAULT;
            double normalized = Math.min(distanceMeters / effectiveRadius, 1.0);
            score += (1.0 - normalized) * 3.0;
        }

        if (store.getTitle() != null && !store.getTitle().isBlank()) {
            score += 0.15;
        }
        if (store.getThumbnail() != null && !store.getThumbnail().isBlank()) {
            score += 0.1;
        }

        return score;
    }

    private double recencyScore(Fp300Store store) {
        LocalDate referenceDate = store.getUpdatedAt() != null ? store.getUpdatedAt() : store.getCreatedAt();
        if (referenceDate == null) {
            return 0.0;
        }

        long daysOld = Math.max(0, ChronoUnit.DAYS.between(referenceDate, LocalDate.now()));
        double decay = Math.min(daysOld, 30) * 0.15;
        return Math.max(0.5, 5.0 - decay);
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

        List<Fp310Place> places = fp310PlaceRepository.findByPlaceIdInAndUseYnAndDeletedAtIsNull(placeIds, FLAG_Y);
        if (places == null || places.isEmpty()) {
            return Collections.emptyMap();
        }

        return places.stream()
                .filter(place -> place.getPlaceId() != null)
                .collect(Collectors.toMap(Fp310Place::getPlaceId, place -> place, (left, right) -> left, LinkedHashMap::new));
    }

    private Double resolveDistanceMeters(
            Fp300Store store,
            Map<String, Fp310Place> placeMap,
            Double lat,
            Double lng
    ) {
        if (lat == null || lng == null || store.getPlaceId() == null) {
            return null;
        }

        Fp310Place place = placeMap.get(store.getPlaceId());
        if (place == null || place.getLatitude() == null || place.getLongitude() == null) {
            return null;
        }

        return calculateDistanceMeters(lat, lng, place.getLatitude(), place.getLongitude());
    }

    private double calculateDistanceMeters(double startLat, double startLng, double endLat, double endLng) {
        double latDistance = Math.toRadians(endLat - startLat);
        double lngDistance = Math.toRadians(endLng - startLng);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(startLat))
                * Math.cos(Math.toRadians(endLat))
                * Math.sin(lngDistance / 2)
                * Math.sin(lngDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }

    private List<ScoredHomeVideoCandidate> diversifyRankedCandidates(List<ScoredHomeVideoCandidate> scored) {
        List<ScoredHomeVideoCandidate> remaining = new ArrayList<>(scored);
        List<ScoredHomeVideoCandidate> diversified = new ArrayList<>(scored.size());
        Map<String, Integer> usernameCounts = new LinkedHashMap<>();
        Map<String, Integer> placeCounts = new LinkedHashMap<>();

        while (!remaining.isEmpty()) {
            ScoredHomeVideoCandidate next = remaining.stream()
                    .max(Comparator.comparingDouble(candidate -> adjustedScore(candidate, usernameCounts, placeCounts)))
                    .orElse(remaining.get(0));

            diversified.add(next);
            remaining.remove(next);

            String username = next.store().getUsername();
            if (username != null && !username.isBlank()) {
                usernameCounts.merge(username, 1, Integer::sum);
            }

            String placeId = next.store().getPlaceId();
            if (placeId != null && !placeId.isBlank()) {
                placeCounts.merge(placeId, 1, Integer::sum);
            }
        }

        return diversified;
    }

    private double adjustedScore(
            ScoredHomeVideoCandidate candidate,
            Map<String, Integer> usernameCounts,
            Map<String, Integer> placeCounts
    ) {
        double adjusted = candidate.baseScore();

        String username = candidate.store().getUsername();
        if (username != null && !username.isBlank()) {
            adjusted -= usernameCounts.getOrDefault(username, 0) * 1.1;
        }

        String placeId = candidate.store().getPlaceId();
        if (placeId != null && !placeId.isBlank()) {
            adjusted -= placeCounts.getOrDefault(placeId, 0) * 0.8;
        }

        return adjusted;
    }

    private void validateNearbyParams(Double lat, Double lng) {
        if (lat == null || lng == null) {
            throw new AppException(ErrorCode.COMMON_MISSING_PARAMETER);
        }
    }

    private double normalizeRadius(Double radius) {
        if (radius == null) {
            return NEARBY_RADIUS_DEFAULT;
        }
        if (radius < NEARBY_RADIUS_MIN) {
            return NEARBY_RADIUS_MIN;
        }
        if (radius > NEARBY_RADIUS_MAX) {
            return NEARBY_RADIUS_MAX;
        }
        return radius;
    }

    public void saveWatchHistory(VideoWatchHistoryCreateRequest req) {
        if (req.getStoreId() == null) {
            throw new IllegalArgumentException("storeId???꾩닔?낅땲??");
        }

        String username = req.getUsername();
        if (username == null || username.isBlank()) {
            throw new AppException(ErrorCode.AUTH_UNAUTHORIZED, "Authenticated username is required");
        }

        boolean isGuest = false;

        Fp303WatchHistory history = new Fp303WatchHistory();
        history.setStoreId(req.getStoreId());
        history.setUsername(username);
        history.setIsGuest(isGuest);
        history.setGuestId(isGuest ? req.getGuestId() : null);

        fp303WatchHistoryRepository.save(history);
    }

    /**
     * Returns the video feed around the requested place/store context.
     */
    public List<VideoFeedItemDTO> getVideoFeed(
            String username,
            Integer storeId,
            String placeId
    ) {
        Set<String> excludedUsernames = loadExcludedUsernames(username);
        List<Fp300Store> resultStores = sanitizeStores(resolveFeedStores(placeId, storeId, FEED_TOTAL_LIMIT));
        if (!excludedUsernames.isEmpty()) {
            resultStores = resultStores.stream()
                    .filter(store -> store.getUsername() == null || !excludedUsernames.contains(store.getUsername()))
                    .toList();
        }
        FeedContext context = loadFeedContext(username, resultStores);

        return resultStores.stream()
                .map(store -> toVideoFeedItemDto(store, context))
                .collect(Collectors.toList());
    }

    private List<Fp300Store> resolveFeedStores(String placeId, Integer storeId, int limit) {
        List<Fp300Store> resultStores = new ArrayList<>();
        int remainLimit = limit;

        if (storeId != null) {
            resultStores.add(findMainStore(storeId));
            remainLimit -= 1;
        }

        Fp310Place centerPlace = findCenterPlace(placeId);
        if (remainLimit > 0 && centerPlace != null) {
            resultStores.addAll(expandRadiusUntilFilled(centerPlace, storeId, remainLimit));
        }

        if (remainLimit > 0 && centerPlace == null) {
            resultStores.addAll(findFallbackStoresByPlaceId(placeId, storeId, remainLimit));
        }

        return resultStores;
    }

    private List<Fp300Store> sanitizeStores(List<Fp300Store> stores) {
        if (stores == null || stores.isEmpty()) {
            return Collections.emptyList();
        }
        return stores.stream()
                .filter(Objects::nonNull)
                .toList();
    }

    private FeedContext loadFeedContext(String username, List<Fp300Store> stores) {
        List<Integer> storeIds = extractStoreIds(stores);
        if (storeIds.isEmpty()) {
            return new FeedContext(Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), Collections.emptySet(), Collections.emptyMap());
        }

        Map<Integer, Long> commentCountMap = defaultMap(loadCommentCountMap(storeIds));
        Map<String, String> profileImageMap = defaultMap(loadProfileImageMap(stores));
        Map<Integer, Long> likeCountMap = defaultMap(likeService.getLikeCountMap(storeIds));
        Set<Integer> myLikedStoreIdSet = defaultSet(likeService.getMyLikedStoreIdSet(username, storeIds));
        Map<String, Fp310Place> placeMap = loadPlaceMap(stores);

        return new FeedContext(commentCountMap, profileImageMap, likeCountMap, myLikedStoreIdSet, placeMap);
    }

    private VideoFeedItemDTO toVideoFeedItemDto(
            Fp300Store store,
            FeedContext context
    ) {
        String title = store.getTitle();
        if (title == null || title.isBlank()) {
            title = store.getStoreName();
        }

        Integer sid = store.getStoreId();

        Long commentCount = context.commentCountMap.getOrDefault(sid, 0L);
        String profileImageUrl = context.profileImageMap.get(store.getUsername());

        Long likeCount = context.likeCountMap.getOrDefault(sid, 0L);
        Boolean likedByMe = context.myLikedStoreIdSet.contains(sid);
        Fp310Place place = store.getPlaceId() == null ? null : context.placeMap.get(store.getPlaceId());
        ResolvedPlace resolvedPlace = contentPlaceResolver.resolve(
                store.getPlaceId(),
                store.getStoreName(),
                store.getAddress()
        );
        Double lat = resolvedPlace.lat() != null ? resolvedPlace.lat() : (place == null ? null : place.getLatitude());
        Double lng = resolvedPlace.lng() != null ? resolvedPlace.lng() : (place == null ? null : place.getLongitude());
        String resolvedPlaceId = resolvedPlace.placeId() != null ? resolvedPlace.placeId() : store.getPlaceId();

        return VideoFeedItemDTO.builder()
                .storeId(sid)
                .placeId(resolvedPlaceId)
                .title(title)
                .storeName(store.getStoreName())
                .address(store.getAddress())
                .lat(lat)
                .lng(lng)
                .fileName(s3UploadService.toVideoUrl(store.getFileName()))
                .thumbnail(s3UploadService.toImageUrl(store.getThumbnail()))
                .videoDuration(store.getVideoDuration())
                .createdAt(store.getCreatedAt())
                .username(store.getUsername())

                .commentCount(commentCount)
                .profileImageUrl(profileImageUrl)

                .likeCount(likeCount)
                .likedByMe(likedByMe)
                .build();
    }

    private Fp310Place findCenterPlace(String placeId) {
        if (placeId == null || placeId.isBlank()) {
            return null;
        }
        return fp310PlaceRepository
                .findByPlaceIdAndUseYnAndDeletedAtIsNull(placeId, FLAG_Y)
                .filter(centerPlace -> centerPlace.getLatitude() != null && centerPlace.getLongitude() != null)
                .orElse(null);
    }

    private Fp300Store findMainStore(Integer storeId) {
        return fp300StoreRepository
                .findByStoreIdAndUseYnAndOpenYnAndDeletedAtIsNull(storeId, FLAG_Y, FLAG_Y)
                .orElseThrow(() -> new IllegalArgumentException("議댁옱?섏? ?딅뒗 storeId: " + storeId));
    }

    private List<Fp300Store> expandRadiusUntilFilled(Fp310Place centerPlace, Integer excludeStoreId, int limit) {
        double radius = DEFAULT_RADIUS_METERS;
        List<Fp300Store> collected = new ArrayList<>();
        Set<Integer> seenIds = new HashSet<>();

        while (collected.size() < limit) {
            int requestLimit = limit - collected.size();
            List<Fp300Store> nearby = findNearbyStores(
                    centerPlace.getLatitude(),
                    centerPlace.getLongitude(),
                    radius,
                    excludeStoreId,
                    requestLimit
            );

            int beforeAdd = collected.size();
            for (Fp300Store store : nearby) {
                Integer sid = store.getStoreId();
                if (sid != null && seenIds.add(sid)) {
                    collected.add(store);
                    if (collected.size() >= limit) {
                        break;
                    }
                }
            }

            if (collected.size() >= limit) {
                break;
            }

            // ???댁긽 ??寃곌낵媛 ?놁쑝硫?猷⑦봽 醫낅즺
            if (collected.size() == beforeAdd) {
                break;
            }

            radius += RADIUS_STEP_METERS;
        }

        return collected;
    }

    private List<Fp300Store> findFallbackStoresByPlaceId(String placeId, Integer excludeStoreId, int limit) {
        if (placeId == null || placeId.isBlank() || limit <= 0) {
            return Collections.emptyList();
        }
        if (excludeStoreId != null) {
            return fp300StoreRepository
                    .findTop9ByPlaceIdAndStoreIdNotAndUseYnAndOpenYnAndDeletedAtIsNullOrderByCreatedAtDesc(
                            placeId,
                            excludeStoreId,
                            FLAG_Y,
                            FLAG_Y
                    )
                    .stream()
                    .limit(limit)
                    .toList();
        }
        return fp300StoreRepository
                .findTop10ByPlaceIdAndUseYnAndOpenYnAndDeletedAtIsNullOrderByCreatedAtDesc(placeId, FLAG_Y, FLAG_Y)
                .stream()
                .limit(limit)
                .toList();
    }

    private Set<String> loadExcludedUsernames(String username) {
        if (username == null || username.isBlank()) {
            return Collections.emptySet();
        }
        Set<String> excluded = new HashSet<>();
        List<String> blocked = blockRepository.findBlockedUsernames(username);
        if (blocked != null) {
            excluded.addAll(blocked);
        }
        List<String> reported = reportRepository.findReportedUsernames(username);
        if (reported != null) {
            excluded.addAll(reported);
        }
        return excluded;
    }

    private List<Fp300Store> findNearbyStores(double lat, double lng, double radius, Integer excludeStoreId, int limit) {
        return fp300StoreRepository.findNearbyStores(
                lat,
                lng,
                radius,
                excludeStoreId,
                limit
        );
    }

    private List<Integer> extractStoreIds(List<Fp300Store> stores) {
        return stores.stream()
                .map(Fp300Store::getStoreId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private Map<Integer, Long> loadCommentCountMap(List<Integer> storeIds) {
        if (storeIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Fp440CommentRepository.StoreCommentCount> rows = fp440CommentRepository.countActiveByStoreIds(storeIds);
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyMap();
        }

        return rows.stream()
                .collect(Collectors.toMap(
                        Fp440CommentRepository.StoreCommentCount::getStoreId,
                        Fp440CommentRepository.StoreCommentCount::getCnt
                ));
    }

    private Map<String, String> loadProfileImageMap(List<Fp300Store> stores) {
        List<String> usernames = stores.stream()
                .map(Fp300Store::getUsername)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (usernames.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Fp100User> users = memberRepository.findByUsernameIn(usernames);
        if (users == null || users.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, String> result = new java.util.LinkedHashMap<>();
        for (Fp100User user : users) {
            String name = user.getUsername();
            if (name == null || name.isBlank()) {
                continue;
            }
            result.putIfAbsent(name, user.getProfileImageUrl());
        }
        return result;
    }

    private <K, V> Map<K, V> defaultMap(Map<K, V> map) {
        return map != null ? map : Collections.emptyMap();
    }

    private <T> Set<T> defaultSet(Set<T> set) {
        return set != null ? set : Collections.emptySet();
    }

    private static class FeedContext {
        private final Map<Integer, Long> commentCountMap;
        private final Map<String, String> profileImageMap;
        private final Map<Integer, Long> likeCountMap;
        private final Set<Integer> myLikedStoreIdSet;
        private final Map<String, Fp310Place> placeMap;

        private FeedContext(
                Map<Integer, Long> commentCountMap,
                Map<String, String> profileImageMap,
                Map<Integer, Long> likeCountMap,
                Set<Integer> myLikedStoreIdSet,
                Map<String, Fp310Place> placeMap
        ) {
            this.commentCountMap = commentCountMap;
            this.profileImageMap = profileImageMap;
            this.likeCountMap = likeCountMap;
            this.myLikedStoreIdSet = myLikedStoreIdSet;
            this.placeMap = placeMap;
        }
    }

    private record ScoredHomeVideoCandidate(
            Fp300Store store,
            double baseScore
    ) {
    }
}
