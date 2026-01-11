// src/main/java/com/plateapp/plate_main/video/service/HomeVideoService.java
package com.plateapp.plate_main.video.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.plateapp.plate_main.like.service.LikeService;
import com.plateapp.plate_main.user.entity.Fp100User;
import com.plateapp.plate_main.user.repository.MemberRepository;
import com.plateapp.plate_main.video.dto.HomeVideoThumbnailDTO;
import com.plateapp.plate_main.video.dto.VideoFeedItemDTO;
import com.plateapp.plate_main.video.dto.VideoWatchHistoryCreateRequest;
import com.plateapp.plate_main.video.entity.Fp300Store;
import com.plateapp.plate_main.video.entity.Fp303WatchHistory;
import com.plateapp.plate_main.video.entity.Fp310Place;
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

    private final Fp300StoreRepository fp300StoreRepository;
    private final Fp303WatchHistoryRepository fp303WatchHistoryRepository;
    private final Fp310PlaceRepository fp310PlaceRepository;

    private final Fp440CommentRepository fp440CommentRepository;
    private final MemberRepository memberRepository;

    // ??ì¶”ê?
    private final LikeService likeService;

    public Page<HomeVideoThumbnailDTO> getHomeVideoThumbnails(
            int page,
            int size,
            String username,
            boolean isGuest,
            String guestId,
            List<String> placeIds
    ) {
        Pageable pageable = PageRequest.of(page, size);

        boolean usePlaceFilter = placeIds != null && !placeIds.isEmpty();
        List<String> safePlaceIds = usePlaceFilter ? placeIds : Collections.singletonList("DUMMY");

        Page<Fp300Store> entityPage =
                fp300StoreRepository.findHomeVideoThumbnails(
                        username,
                        isGuest,
                        guestId,
                        usePlaceFilter,
                        safePlaceIds,
                        pageable
                );

        return entityPage.map(this::toThumbnailDto);
    }

    private HomeVideoThumbnailDTO toThumbnailDto(Fp300Store e) {
        return HomeVideoThumbnailDTO.builder()
                .storeId(e.getStoreId())
                .title(e.getTitle())
                .fileName(e.getFileName())
                .thumbnail(e.getThumbnail())
                .videoDuration(e.getVideoDuration())
                .muteYn(e.getMuteYn())
                .videoSize(e.getVideoSize())
                .storeName(e.getStoreName())
                .address(e.getAddress())
                .placeId(e.getPlaceId())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    public void saveWatchHistory(VideoWatchHistoryCreateRequest req) {
        if (req.getStoreId() == null) {
            throw new IllegalArgumentException("storeId???„ìˆ˜?…ë‹ˆ??");
        }

        String username = req.getUsername();
        if (username == null || username.isBlank()) {
            String guestId = req.getGuestId() != null ? req.getGuestId() : "UNKNOWN";
            username = "GUEST_" + guestId;
        }

        boolean isGuest = Boolean.TRUE.equals(req.getIsGuest());

        Fp303WatchHistory history = new Fp303WatchHistory();
        history.setStoreId(req.getStoreId());
        history.setUsername(username);
        history.setIsGuest(isGuest);
        history.setGuestId(isGuest ? req.getGuestId() : null);

        fp303WatchHistoryRepository.save(history);
    }

    /**
     * ?”¹ ?„ì¹˜ ê¸°ë°˜ ?™ì˜???¼ë“œ ì¡°íšŒ (ë°˜ê²½ ê²€??ë²„ì „)
     * + ?“ê? ??/ ?…ë¡œ???„ë¡œ???´ë?ì§€ / ??ì¢‹ì•„????& ?´ê? ì¢‹ì•„???¬ë? ?¬í•¨
     */
    public List<VideoFeedItemDTO> getVideoFeed(
            String username,
            Integer storeId,
            String placeId
    ) {
        Fp310Place centerPlace = findCenterPlace(placeId);

        List<Fp300Store> resultStores = new ArrayList<>();
        int remainLimit = FEED_TOTAL_LIMIT;

        if (storeId != null) {
            resultStores.add(findMainStore(storeId));
            remainLimit -= 1;
        }

        if (remainLimit > 0) {
            resultStores.addAll(expandRadiusUntilFilled(centerPlace, storeId, remainLimit));
        }

        List<Integer> storeIds = extractStoreIds(resultStores);
        Map<Integer, Long> commentCountMap = loadCommentCountMap(storeIds);
        Map<String, String> profileImageMap = loadProfileImageMap(resultStores);
        Map<Integer, Long> likeCountMap = likeService.getLikeCountMap(storeIds);
        Set<Integer> myLikedStoreIdSet = likeService.getMyLikedStoreIdSet(username, storeIds);

        return resultStores.stream()
                .map(store -> toVideoFeedItemDto(store, commentCountMap, profileImageMap, likeCountMap, myLikedStoreIdSet))
                .collect(Collectors.toList());
    }

    private VideoFeedItemDTO toVideoFeedItemDto(
            Fp300Store store,
            Map<Integer, Long> commentCountMap,
            Map<String, String> profileImageMap,
            Map<Integer, Long> likeCountMap,
            Set<Integer> myLikedStoreIdSet
    ) {
        String title = store.getTitle();
        if (title == null || title.isBlank()) {
            title = store.getStoreName();
        }

        Integer sid = store.getStoreId();

        Long commentCount = commentCountMap.getOrDefault(sid, 0L);
        String profileImageUrl = profileImageMap.get(store.getUsername());

        Long likeCount = likeCountMap.getOrDefault(sid, 0L);
        Boolean likedByMe = myLikedStoreIdSet.contains(sid);

        return VideoFeedItemDTO.builder()
                .storeId(sid)
                .placeId(store.getPlaceId())
                .title(title)
                .storeName(store.getStoreName())
                .address(store.getAddress())
                .fileName(store.getFileName())
                .thumbnail(store.getThumbnail())
                .videoDuration(store.getVideoDuration())
                .username(store.getUsername())

                .commentCount(commentCount)
                .profileImageUrl(profileImageUrl)

                // ??ì¢‹ì•„??ì£¼ìž…
                .likeCount(likeCount)
                .likedByMe(likedByMe)
                .build();
    }

    private Fp310Place findCenterPlace(String placeId) {
        Fp310Place centerPlace = fp310PlaceRepository
                .findByPlaceIdAndUseYnAndDeletedAtIsNull(placeId, FLAG_Y)
                .orElseThrow(() -> new IllegalArgumentException("ì¢Œí‘œ ?•ë³´ë¥?ì°¾ì„ ???†ëŠ” placeId: " + placeId));

        if (centerPlace.getLatitude() == null || centerPlace.getLongitude() == null) {
            throw new IllegalStateException("placeId???„ë„/ê²½ë„ ?•ë³´ê°€ ?†ìŠµ?ˆë‹¤: " + placeId);
        }
        return centerPlace;
    }

    private Fp300Store findMainStore(Integer storeId) {
        return fp300StoreRepository
                .findByStoreIdAndUseYnAndOpenYnAndDeletedAtIsNull(storeId, FLAG_Y, FLAG_Y)
                .orElseThrow(() -> new IllegalArgumentException("ì¡´ìž¬?˜ì? ?ŠëŠ” storeId: " + storeId));
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

            // 더 이상 새 결과가 없으면 루프 종료
            if (collected.size() == beforeAdd) {
                break;
            }

            radius += RADIUS_STEP_METERS;
        }

        return collected;
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

        return fp440CommentRepository.countActiveByStoreIds(storeIds).stream()
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

        return memberRepository.findByUsernameIn(usernames).stream()
                .collect(Collectors.toMap(
                        Fp100User::getUsername,
                        Fp100User::getProfileImageUrl,
                        (a, b) -> a
                ));
    }
}
