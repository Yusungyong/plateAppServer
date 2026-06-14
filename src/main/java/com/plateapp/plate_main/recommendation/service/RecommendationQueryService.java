package com.plateapp.plate_main.recommendation.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plateapp.plate_main.common.s3.S3UploadService;
import com.plateapp.plate_main.feed.entity.Fp400Feed;
import com.plateapp.plate_main.feed.repository.Fp400FeedRepository;
import com.plateapp.plate_main.friend.repository.Fp200VisitRepository;
import com.plateapp.plate_main.like.repository.FeedLikeRepository;
import com.plateapp.plate_main.like.service.LikeService;
import com.plateapp.plate_main.recommendation.dto.RecommendationItem;
import com.plateapp.plate_main.recommendation.dto.RecommendationResponse;
import com.plateapp.plate_main.recommendation.dto.RecommendationScoreBreakdown;
import com.plateapp.plate_main.recommendation.dto.RecommendationSection;
import com.plateapp.plate_main.video.entity.Fp300Store;
import com.plateapp.plate_main.video.entity.Fp310Place;
import com.plateapp.plate_main.video.repository.Fp300StoreRepository;
import com.plateapp.plate_main.video.repository.Fp310PlaceRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RecommendationQueryService {

    private static final int DEFAULT_LIMIT_PER_SURFACE = 6;
    private static final int MAX_LIMIT_PER_SURFACE = 20;
    private static final int CANDIDATE_MULTIPLIER = 4;
    private static final double DEFAULT_RADIUS_METERS = 3_000.0;
    private static final String TARGET_STORE = "STORE";
    private static final String TARGET_IMAGE_FEED = "IMAGE_FEED";
    private static final String TARGET_VIDEO_FEED = "VIDEO_FEED";
    private static final String TARGET_SEASONAL_MENU = "SEASONAL_MENU";

    private static final List<String> DEFAULT_SURFACES = List.of("HOME_FEED", "NEARBY", "SEASONAL", "FRIEND");
    private static final Set<String> SUPPORTED_SURFACES = Set.of(
            "HOME_FEED",
            "NEARBY",
            "SEASONAL",
            "FRIEND",
            "STORE_DETAIL_SIMILAR"
    );

    private final Fp300StoreRepository storeRepository;
    private final Fp400FeedRepository feedRepository;
    private final Fp310PlaceRepository placeRepository;
    private final Fp200VisitRepository visitRepository;
    private final LikeService likeService;
    private final FeedLikeRepository feedLikeRepository;
    private final S3UploadService s3UploadService;

    @Transactional(readOnly = true)
    public RecommendationResponse getRecommendations(
            String surfaces,
            Integer limitPerSurface,
            Double lat,
            Double lng,
            Integer currentMonth,
            Integer baseStoreId,
            Integer baseFeedId,
            String usernameParam,
            Boolean isGuest,
            String guestId,
            Authentication authentication
    ) {
        String username = resolveUsername(authentication, usernameParam, Boolean.TRUE.equals(isGuest));
        int safeLimit = safeLimit(limitPerSurface);
        int month = safeMonth(currentMonth);

        List<RecommendationSection> sections = resolveSurfaces(surfaces).stream()
                .map(surface -> buildSection(surface, safeLimit, lat, lng, month, baseStoreId, baseFeedId, username, guestId))
                .toList();

        return new RecommendationResponse(
                "reco-" + UUID.randomUUID(),
                LocalDateTime.now(),
                sections
        );
    }

    private RecommendationSection buildSection(
            String surface,
            int limit,
            Double lat,
            Double lng,
            int month,
            Integer baseStoreId,
            Integer baseFeedId,
            String username,
            String guestId
    ) {
        return switch (surface) {
            case "NEARBY" -> new RecommendationSection(
                    surface,
                    "내 주변 추천",
                    "현재 위치와 가까운 맛집과 피드를 우선 추천합니다.",
                    buildNearbyItems(surface, limit, lat, lng)
            );
            case "SEASONAL" -> new RecommendationSection(
                    surface,
                    month + "월 제철 추천",
                    "이번 달에 먹기 좋은 제철 메뉴입니다.",
                    buildSeasonalItems(surface, limit, month)
            );
            case "FRIEND" -> new RecommendationSection(
                    surface,
                    "친구 기반 추천",
                    "친구 방문 기록과 최근 인기 데이터를 함께 반영했습니다.",
                    buildFriendItems(surface, limit, username, lat, lng)
            );
            case "STORE_DETAIL_SIMILAR" -> new RecommendationSection(
                    surface,
                    "비슷한 맛집",
                    "현재 보고 있는 매장이나 피드와 연관된 맛집입니다.",
                    buildSimilarStoreItems(surface, limit, baseStoreId, baseFeedId, lat, lng)
            );
            default -> new RecommendationSection(
                    "HOME_FEED",
                    "오늘의 추천",
                    "최근 인기 콘텐츠와 위치 신호를 조합한 추천입니다.",
                    buildHomeFeedItems("HOME_FEED", limit, lat, lng)
            );
        };
    }

    private List<RecommendationItem> buildHomeFeedItems(String surface, int limit, Double lat, Double lng) {
        int candidateLimit = limit * CANDIDATE_MULTIPLIER;
        List<Fp300Store> stores = latestStores(candidateLimit);
        List<Fp400Feed> feeds = latestFeeds(candidateLimit);
        RecommendationContext context = loadContext(stores, feeds, lat, lng);

        List<ScoredItem> candidates = new ArrayList<>();
        for (Fp300Store store : stores) {
            candidates.add(toVideoFeedItem(surface, store, context, 0, 0, 0, List.of("최근 인기")));
        }
        for (Fp400Feed feed : feeds) {
            candidates.add(toImageFeedItem(surface, feed, context, 0, 0, 0, List.of("최근 인기")));
        }
        return topItems(candidates, limit);
    }

    private List<RecommendationItem> buildNearbyItems(String surface, int limit, Double lat, Double lng) {
        int candidateLimit = limit * CANDIDATE_MULTIPLIER;
        List<Fp300Store> stores;
        List<Fp400Feed> feeds;
        if (hasLocation(lat, lng)) {
            stores = storeRepository.findNearbyStores(lat, lng, DEFAULT_RADIUS_METERS, null, candidateLimit);
            feeds = feedRepository.findNearbyForHomeByGroup(
                    lat,
                    lng,
                    DEFAULT_RADIUS_METERS,
                    null,
                    null,
                    PageRequest.of(0, candidateLimit)
            );
        } else {
            stores = latestStores(candidateLimit);
            feeds = latestFeeds(candidateLimit);
        }

        RecommendationContext context = loadContext(stores, feeds, lat, lng);
        List<ScoredItem> candidates = new ArrayList<>();
        for (Fp300Store store : stores) {
            int nearby = nearbyScore(distanceMeters(store.getPlaceId(), context));
            List<String> reasons = nearby > 0 ? List.of("가까운 위치", "최근 인기") : List.of("인기 추천");
            candidates.add(toStoreItem(surface, store, context, nearby, 0, 0, reasons));
        }
        for (Fp400Feed feed : feeds) {
            int nearby = nearbyScore(distanceMeters(feed.getPlaceId(), context));
            List<String> reasons = nearby > 0 ? List.of("가까운 위치", "최근 인기") : List.of("인기 추천");
            candidates.add(toImageFeedItem(surface, feed, context, nearby, 0, 0, reasons));
        }
        return topItems(candidates, limit);
    }

    private List<RecommendationItem> buildFriendItems(
            String surface,
            int limit,
            String username,
            Double lat,
            Double lng
    ) {
        if (!hasText(username)) {
            return buildFriendFallbackItems(surface, limit, lat, lng);
        }

        List<Fp200VisitRepository.RecentStoreRow> rows = visitRepository.findRecentStores(
                username,
                limit * CANDIDATE_MULTIPLIER
        );
        List<Integer> storeIds = rows.stream()
                .map(Fp200VisitRepository.RecentStoreRow::getStoreId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (storeIds.isEmpty()) {
            return buildFriendFallbackItems(surface, limit, lat, lng);
        }

        Map<Integer, Fp300Store> storeMap = storeRepository.findByStoreIdIn(storeIds).stream()
                .collect(Collectors.toMap(Fp300Store::getStoreId, Function.identity(), (left, right) -> left));
        Map<Integer, List<String>> friendNames =
                friendNamesByStore(visitRepository.findFriendVisitsForStores(username, storeIds));
        List<Fp300Store> stores = rows.stream()
                .map(row -> storeMap.get(row.getStoreId()))
                .filter(Objects::nonNull)
                .toList();
        RecommendationContext context = loadContext(stores, List.of(), lat, lng);

        List<ScoredItem> candidates = new ArrayList<>();
        for (Fp200VisitRepository.RecentStoreRow row : rows) {
            Fp300Store store = storeMap.get(row.getStoreId());
            if (store == null) {
                continue;
            }
            int friendSignal = Math.min(20, Math.max(10, intValue(row.getVisitCount()) * 10));
            List<String> names = distinctLimit(friendNames.get(row.getStoreId()), 3);
            List<String> reasons = names.isEmpty() ? List.of("친구 방문 맛집") : List.of("친구 방문 맛집", "친구 반응");
            candidates.add(toStoreItem(surface, store, context, 0, friendSignal, 0, reasons, names));
        }
        return topItems(candidates, limit);
    }

    private List<RecommendationItem> buildFriendFallbackItems(String surface, int limit, Double lat, Double lng) {
        List<Fp300Store> stores = latestStores(limit * CANDIDATE_MULTIPLIER);
        RecommendationContext context = loadContext(stores, List.of(), lat, lng);
        List<ScoredItem> candidates = stores.stream()
                .map(store -> toStoreItem(surface, store, context, 0, 0, 0, List.of("인기 추천")))
                .toList();
        return topItems(candidates, limit);
    }

    private List<RecommendationItem> buildSimilarStoreItems(
            String surface,
            int limit,
            Integer baseStoreId,
            Integer baseFeedId,
            Double lat,
            Double lng
    ) {
        String placeId = null;
        Integer excludeStoreId = null;

        if (baseStoreId != null) {
            Fp300Store baseStore = storeRepository.findById(baseStoreId).orElse(null);
            if (baseStore != null) {
                placeId = baseStore.getPlaceId();
                excludeStoreId = baseStore.getStoreId();
            }
        }
        if (!hasText(placeId) && baseFeedId != null) {
            Fp400Feed baseFeed = feedRepository.findById(baseFeedId).orElse(null);
            if (baseFeed != null) {
                placeId = baseFeed.getPlaceId();
            }
        }

        List<Fp300Store> stores;
        if (hasText(placeId) && excludeStoreId != null) {
            stores = storeRepository.findTop9ByPlaceIdAndStoreIdNotAndUseYnAndOpenYnAndDeletedAtIsNullOrderByCreatedAtDesc(
                    placeId,
                    excludeStoreId,
                    "Y",
                    "Y"
            );
        } else if (hasText(placeId)) {
            stores = storeRepository.findTop10ByPlaceIdAndUseYnAndOpenYnAndDeletedAtIsNullOrderByCreatedAtDesc(
                    placeId,
                    "Y",
                    "Y"
            );
        } else {
            stores = latestStores(limit * CANDIDATE_MULTIPLIER);
        }

        RecommendationContext context = loadContext(stores, List.of(), lat, lng);
        List<ScoredItem> candidates = stores.stream()
                .map(store -> toStoreItem(surface, store, context, 0, 0, 15, List.of("비슷한 장소", "최근 인기")))
                .toList();
        return topItems(candidates, limit);
    }

    private List<RecommendationItem> buildSeasonalItems(String surface, int limit, int month) {
        List<SeasonalFallback> seasonalFoods = seasonalFallbacks(month);
        List<ScoredItem> candidates = new ArrayList<>();
        for (int i = 0; i < seasonalFoods.size(); i++) {
            SeasonalFallback food = seasonalFoods.get(i);
            int seasonalFoodId = month * 100 + i + 1;
            RecommendationScoreBreakdown breakdown = new RecommendationScoreBreakdown(0, 0, 0, 5, 15, 0, 0);
            int score = totalScore(breakdown);
            RecommendationItem item = new RecommendationItem(
                    surface + ":seasonal:" + seasonalFoodId,
                    surface,
                    TARGET_SEASONAL_MENU,
                    food.name(),
                    month + "월에 먹기 좋은 제철 메뉴입니다.",
                    null,
                    null,
                    null,
                    null,
                    seasonalFoodId,
                    null,
                    null,
                    "제철음식",
                    null,
                    null,
                    List.of(),
                    score,
                    breakdown,
                    List.of("제철 메뉴", food.reason()),
                    false
            );
            candidates.add(new ScoredItem(item, score));
        }
        return topItems(candidates, limit);
    }

    private ScoredItem toStoreItem(
            String surface,
            Fp300Store store,
            RecommendationContext context,
            int nearby,
            int friendSignal,
            int similarity,
            List<String> reasonLabels
    ) {
        return toStoreItem(surface, store, context, nearby, friendSignal, similarity, reasonLabels, List.of());
    }

    private ScoredItem toStoreItem(
            String surface,
            Fp300Store store,
            RecommendationContext context,
            int nearby,
            int friendSignal,
            int similarity,
            List<String> reasonLabels,
            List<String> friendNames
    ) {
        Integer storeId = store.getStoreId();
        int popularity = popularityScore(context.storeLikeCounts().get(storeId), toDateTime(store.getCreatedAt()));
        RecommendationScoreBreakdown breakdown =
                new RecommendationScoreBreakdown(nearby, 0, friendSignal, popularity, 0, similarity, 0);
        int score = totalScore(breakdown);
        RecommendationItem item = new RecommendationItem(
                surface + ":store:" + storeId,
                surface,
                TARGET_STORE,
                firstText(store.getStoreName(), store.getTitle()),
                firstText(store.getAddress(), "추천 맛집입니다."),
                storeId,
                store.getPlaceId(),
                null,
                null,
                null,
                store.getStoreName(),
                store.getAddress(),
                null,
                s3UploadService.toImageUrl(store.getThumbnail()),
                distanceMeters(store.getPlaceId(), context),
                friendNames,
                score,
                breakdown,
                reasonLabels,
                false
        );
        return new ScoredItem(item, score);
    }

    private ScoredItem toVideoFeedItem(
            String surface,
            Fp300Store store,
            RecommendationContext context,
            int nearby,
            int friendSignal,
            int similarity,
            List<String> reasonLabels
    ) {
        Integer storeId = store.getStoreId();
        int popularity = popularityScore(context.storeLikeCounts().get(storeId), toDateTime(store.getCreatedAt()));
        RecommendationScoreBreakdown breakdown =
                new RecommendationScoreBreakdown(nearby, 0, friendSignal, popularity, 0, similarity, 0);
        int score = totalScore(breakdown);
        RecommendationItem item = new RecommendationItem(
                surface + ":video:" + storeId,
                surface,
                TARGET_VIDEO_FEED,
                firstText(store.getTitle(), store.getStoreName()),
                firstText(store.getStoreName(), store.getAddress()),
                storeId,
                store.getPlaceId(),
                null,
                storeId,
                null,
                store.getStoreName(),
                store.getAddress(),
                null,
                s3UploadService.toImageUrl(store.getThumbnail()),
                distanceMeters(store.getPlaceId(), context),
                List.of(),
                score,
                breakdown,
                reasonLabels,
                false
        );
        return new ScoredItem(item, score);
    }

    private ScoredItem toImageFeedItem(
            String surface,
            Fp400Feed feed,
            RecommendationContext context,
            int nearby,
            int friendSignal,
            int similarity,
            List<String> reasonLabels
    ) {
        Integer feedId = feed.getFeedNo();
        int popularity = popularityScore(context.feedLikeCounts().get(feedId), feed.getCreatedAt());
        RecommendationScoreBreakdown breakdown =
                new RecommendationScoreBreakdown(nearby, 0, friendSignal, popularity, 0, similarity, 0);
        int score = totalScore(breakdown);
        RecommendationItem item = new RecommendationItem(
                surface + ":image:" + feedId,
                surface,
                TARGET_IMAGE_FEED,
                firstText(feed.getFeedTitle(), feed.getStoreName()),
                firstText(feed.getContent(), feed.getLocation()),
                null,
                feed.getPlaceId(),
                feedId,
                null,
                null,
                feed.getStoreName(),
                feed.getLocation(),
                null,
                s3UploadService.toFeedImageUrl(primaryFeedImage(feed)),
                distanceMeters(feed.getPlaceId(), context),
                List.of(),
                score,
                breakdown,
                reasonLabels,
                false
        );
        return new ScoredItem(item, score);
    }

    private RecommendationContext loadContext(
            Collection<Fp300Store> stores,
            Collection<Fp400Feed> feeds,
            Double lat,
            Double lng
    ) {
        List<Integer> storeIds = stores.stream()
                .map(Fp300Store::getStoreId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<Integer> feedIds = feeds.stream()
                .map(Fp400Feed::getFeedNo)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Set<String> placeIds = new LinkedHashSet<>();
        stores.stream().map(Fp300Store::getPlaceId).filter(this::hasText).forEach(placeIds::add);
        feeds.stream().map(Fp400Feed::getPlaceId).filter(this::hasText).forEach(placeIds::add);

        Map<Integer, Long> storeLikeCounts = likeService.getLikeCountMap(storeIds);
        Map<Integer, Long> feedLikeCounts = loadFeedLikeCounts(feedIds);
        Map<String, Fp310Place> placeMap = loadPlaceMap(placeIds);
        return new RecommendationContext(storeLikeCounts, feedLikeCounts, placeMap, lat, lng);
    }

    private Map<Integer, Long> loadFeedLikeCounts(List<Integer> feedIds) {
        if (feedIds.isEmpty()) {
            return Map.of();
        }
        return feedLikeRepository.countActiveByFeedIds(feedIds).stream()
                .collect(Collectors.toMap(
                        FeedLikeRepository.FeedLikeCount::getFeedId,
                        FeedLikeRepository.FeedLikeCount::getCnt
                ));
    }

    private Map<String, Fp310Place> loadPlaceMap(Set<String> placeIds) {
        if (placeIds.isEmpty()) {
            return Map.of();
        }
        return placeRepository.findByPlaceIdInAndUseYnAndDeletedAtIsNull(placeIds, "Y").stream()
                .filter(place -> hasText(place.getPlaceId()))
                .collect(Collectors.toMap(Fp310Place::getPlaceId, Function.identity(), (left, right) -> left));
    }

    private List<RecommendationItem> topItems(List<ScoredItem> candidates, int limit) {
        return candidates.stream()
                .filter(item -> item.item().title() != null && !item.item().title().isBlank())
                .sorted(Comparator.comparingInt(ScoredItem::score).reversed())
                .limit(limit)
                .map(ScoredItem::item)
                .toList();
    }

    private List<Fp300Store> latestStores(int limit) {
        return storeRepository.findLatestForHome(PageRequest.of(0, Math.max(1, limit)));
    }

    private List<Fp400Feed> latestFeeds(int limit) {
        return feedRepository.findLatestForHomeByGroup(null, null, PageRequest.of(0, Math.max(1, limit)));
    }

    private Map<Integer, List<String>> friendNamesByStore(List<Fp200VisitRepository.RecentFriendVisitRow> rows) {
        Map<Integer, List<String>> result = new HashMap<>();
        for (Fp200VisitRepository.RecentFriendVisitRow row : rows) {
            if (row.getStoreId() == null || !hasText(row.getFriendName())) {
                continue;
            }
            result.computeIfAbsent(row.getStoreId(), ignored -> new ArrayList<>()).add(row.getFriendName());
        }
        return result;
    }

    private int popularityScore(Long likeCount, LocalDateTime createdAt) {
        long likes = likeCount == null ? 0L : likeCount;
        return clamp((int) Math.min(10, likes * 2) + freshnessBonus(createdAt), 0, 15);
    }

    private int freshnessBonus(LocalDateTime createdAt) {
        if (createdAt == null) {
            return 3;
        }
        long hours = Math.max(0, ChronoUnit.HOURS.between(createdAt, LocalDateTime.now()));
        if (hours <= 24) {
            return 5;
        }
        if (hours <= 168) {
            return 4;
        }
        if (hours <= 720) {
            return 3;
        }
        return 2;
    }

    private int nearbyScore(Integer distanceM) {
        if (distanceM == null) {
            return 0;
        }
        if (distanceM <= 1_000) {
            return 30;
        }
        if (distanceM <= 3_000) {
            return 20;
        }
        return 8;
    }

    private int totalScore(RecommendationScoreBreakdown breakdown) {
        int total = intValue(breakdown.nearby())
                + intValue(breakdown.categoryAffinity())
                + intValue(breakdown.friendSignal())
                + intValue(breakdown.popularity())
                + intValue(breakdown.seasonal())
                + intValue(breakdown.similarity())
                - intValue(breakdown.seenPenalty());
        return clamp(total, 0, 100);
    }

    private Integer distanceMeters(String placeId, RecommendationContext context) {
        if (!hasLocation(context.lat(), context.lng()) || !hasText(placeId)) {
            return null;
        }
        Fp310Place place = context.placeMap().get(placeId);
        if (place == null || place.getLatitude() == null || place.getLongitude() == null) {
            return null;
        }
        return (int) Math.round(haversineMeters(context.lat(), context.lng(), place.getLatitude(), place.getLongitude()));
    }

    private double haversineMeters(double lat1, double lng1, double lat2, double lng2) {
        double earthRadiusM = 6_371_000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusM * c;
    }

    private String primaryFeedImage(Fp400Feed feed) {
        List<String> images = parseImages(feed.getImages());
        String primary = images.isEmpty() ? feed.getThumbnail() : images.get(0);
        return buildThumbnailRelativePath(primary);
    }

    private List<String> parseImages(String images) {
        if (!hasText(images)) {
            return List.of();
        }
        return java.util.Arrays.stream(images.split(","))
                .map(String::trim)
                .filter(this::hasText)
                .distinct()
                .toList();
    }

    private String buildThumbnailRelativePath(String relativePath) {
        if (!hasText(relativePath)) {
            return null;
        }
        int slash = relativePath.indexOf('/');
        if (slash <= 0 || slash == relativePath.length() - 1) {
            return relativePath;
        }
        String datePrefix = relativePath.substring(0, slash);
        String filename = relativePath.substring(slash + 1);
        return datePrefix + "/thumbnails/300x300/" + filename;
    }

    private List<String> resolveSurfaces(String rawSurfaces) {
        if (!hasText(rawSurfaces)) {
            return DEFAULT_SURFACES;
        }
        List<String> result = java.util.Arrays.stream(rawSurfaces.split(","))
                .map(String::trim)
                .filter(this::hasText)
                .map(String::toUpperCase)
                .filter(SUPPORTED_SURFACES::contains)
                .distinct()
                .toList();
        return result.isEmpty() ? DEFAULT_SURFACES : result;
    }

    private int safeLimit(Integer limitPerSurface) {
        if (limitPerSurface == null) {
            return DEFAULT_LIMIT_PER_SURFACE;
        }
        return clamp(limitPerSurface, 1, MAX_LIMIT_PER_SURFACE);
    }

    private int safeMonth(Integer currentMonth) {
        int month = currentMonth == null ? LocalDate.now().getMonthValue() : currentMonth;
        return clamp(month, 1, 12);
    }

    private String resolveUsername(Authentication authentication, String usernameParam, boolean isGuest) {
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() != null) {
            String principal = String.valueOf(authentication.getPrincipal());
            if (hasText(principal) && !"anonymousUser".equals(principal)) {
                return principal;
            }
        }
        if (isGuest) {
            return null;
        }
        return hasText(usernameParam) ? usernameParam.trim() : null;
    }

    private List<SeasonalFallback> seasonalFallbacks(int month) {
        return switch (month) {
            case 1 -> List.of(
                    new SeasonalFallback("굴", "겨울에 맛이 깊은 해산물"),
                    new SeasonalFallback("대구", "맑은 탕과 구이에 잘 어울림"),
                    new SeasonalFallback("배추", "겨울 김치와 전골에 적합")
            );
            case 2 -> List.of(
                    new SeasonalFallback("딸기", "겨울 끝 무렵 당도가 좋음"),
                    new SeasonalFallback("꼬막", "쫄깃한 식감이 좋은 제철 해산물"),
                    new SeasonalFallback("우엉", "조림과 튀김에 잘 어울림")
            );
            case 3 -> List.of(
                    new SeasonalFallback("주꾸미", "봄철 대표 해산물"),
                    new SeasonalFallback("냉이", "향이 좋은 봄나물"),
                    new SeasonalFallback("달래", "무침과 된장국에 잘 어울림")
            );
            case 4 -> List.of(
                    new SeasonalFallback("두릅", "봄 향이 진한 산나물"),
                    new SeasonalFallback("참다랑어", "봄철 별미로 추천"),
                    new SeasonalFallback("취나물", "비빔밥과 나물 반찬에 적합")
            );
            case 5 -> List.of(
                    new SeasonalFallback("멍게", "향이 선명한 봄 해산물"),
                    new SeasonalFallback("장어", "초여름 보양 메뉴로 추천"),
                    new SeasonalFallback("매실", "음료와 장아찌에 잘 어울림")
            );
            case 6 -> List.of(
                    new SeasonalFallback("감자", "초여름에 포슬한 식감이 좋음"),
                    new SeasonalFallback("참외", "더운 날 가볍게 먹기 좋음"),
                    new SeasonalFallback("장어", "여름 보양식으로 인기가 높음")
            );
            case 7 -> List.of(
                    new SeasonalFallback("옥수수", "여름 간식과 구이에 적합"),
                    new SeasonalFallback("복숭아", "여름 대표 과일"),
                    new SeasonalFallback("민어", "여름 보양 생선으로 추천")
            );
            case 8 -> List.of(
                    new SeasonalFallback("전복", "여름 보양 메뉴와 잘 어울림"),
                    new SeasonalFallback("토마토", "샐러드와 파스타에 적합"),
                    new SeasonalFallback("포도", "8월에 당도가 좋은 과일")
            );
            case 9 -> List.of(
                    new SeasonalFallback("꽃게", "가을 대표 해산물"),
                    new SeasonalFallback("고등어", "구이와 조림에 잘 어울림"),
                    new SeasonalFallback("배", "가을 과일로 추천")
            );
            case 10 -> List.of(
                    new SeasonalFallback("전어", "가을 별미 생선"),
                    new SeasonalFallback("버섯", "전골과 구이에 잘 어울림"),
                    new SeasonalFallback("무", "국물 요리와 조림에 적합")
            );
            case 11 -> List.of(
                    new SeasonalFallback("과메기", "늦가을부터 맛보기 좋음"),
                    new SeasonalFallback("꼬막", "겨울 초입 대표 해산물"),
                    new SeasonalFallback("유자", "차와 소스에 잘 어울림")
            );
            default -> List.of(
                    new SeasonalFallback("방어", "겨울에 기름기가 좋은 생선"),
                    new SeasonalFallback("굴", "겨울 대표 해산물"),
                    new SeasonalFallback("귤", "겨울 과일로 부담 없이 즐김")
            );
        };
    }

    private List<String> distinctLimit(List<String> values, int limit) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(this::hasText)
                .distinct()
                .limit(limit)
                .toList();
    }

    private LocalDateTime toDateTime(LocalDate date) {
        return date == null ? null : date.atStartOfDay();
    }

    private String firstText(String first, String second) {
        return hasText(first) ? first : second;
    }

    private boolean hasLocation(Double lat, Double lng) {
        return lat != null && lng != null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private int intValue(Number value) {
        return value == null ? 0 : value.intValue();
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private record RecommendationContext(
            Map<Integer, Long> storeLikeCounts,
            Map<Integer, Long> feedLikeCounts,
            Map<String, Fp310Place> placeMap,
            Double lat,
            Double lng
    ) {
    }

    private record ScoredItem(
            RecommendationItem item,
            int score
    ) {
    }

    private record SeasonalFallback(
            String name,
            String reason
    ) {
    }
}
