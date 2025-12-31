// src/main/java/com/plateapp/plate_main/video/service/HomeVideoService.java
package com.plateapp.plate_main.video.service;

import java.util.ArrayList;
import java.util.Collections;
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

    private final Fp300StoreRepository fp300StoreRepository;
    private final Fp303WatchHistoryRepository fp303WatchHistoryRepository;
    private final Fp310PlaceRepository fp310PlaceRepository;

    private final Fp440CommentRepository fp440CommentRepository;
    private final MemberRepository memberRepository;

    // ✅ 추가
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
            throw new IllegalArgumentException("storeId는 필수입니다.");
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
     * 🔹 위치 기반 동영상 피드 조회 (반경 검색 버전)
     * + 댓글 수 / 업로더 프로필 이미지 / ✅ 좋아요 수 & 내가 좋아요 여부 포함
     */
    public List<VideoFeedItemDTO> getVideoFeed(
            String username,
            Integer storeId,
            String placeId
    ) {
        final String USE_Y = "Y";
        final String OPEN_Y = "Y";
        final double RADIUS_METERS = 2000.0;
        final int TOTAL_LIMIT = 10;

        // 1) 중심 좌표 가져오기 (fp_310)
        Fp310Place centerPlace = fp310PlaceRepository
                .findByPlaceIdAndUseYnAndDeletedAtIsNull(placeId, "Y")
                .orElseThrow(() ->
                        new IllegalArgumentException("좌표 정보를 찾을 수 없는 placeId: " + placeId));

        if (centerPlace.getLatitude() == null || centerPlace.getLongitude() == null) {
            throw new IllegalStateException("placeId의 위도/경도 정보가 없습니다: " + placeId);
        }

        double centerLat = centerPlace.getLatitude();
        double centerLng = centerPlace.getLongitude();

        List<Fp300Store> resultStores = new ArrayList<>();

        // 2) 기준 store (최초 진입 시)
        int remainLimit = TOTAL_LIMIT;
        if (storeId != null) {
            Fp300Store mainStore = fp300StoreRepository
                    .findByStoreIdAndUseYnAndOpenYnAndDeletedAtIsNull(storeId, USE_Y, OPEN_Y)
                    .orElseThrow(() ->
                            new IllegalArgumentException("존재하지 않는 storeId: " + storeId));
            resultStores.add(mainStore);
            remainLimit -= 1;
        }

        // 3) 반경 안의 다른 가게들 (거리순)
        if (remainLimit > 0) {
            Integer excludeStoreId = storeId;
            List<Fp300Store> nearby = fp300StoreRepository.findNearbyStores(
                    centerLat,
                    centerLng,
                    RADIUS_METERS,
                    excludeStoreId,
                    remainLimit
            );
            resultStores.addAll(nearby);
        }

        // ✅ storeIds 준비
        List<Integer> storeIds = resultStores.stream()
                .map(Fp300Store::getStoreId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        // ✅ (A) 댓글 카운트 배치 조회 (storeId -> count)
        Map<Integer, Long> commentCountMap = storeIds.isEmpty()
                ? Collections.emptyMap()
                : fp440CommentRepository.countActiveByStoreIds(storeIds).stream()
                .collect(Collectors.toMap(
                        Fp440CommentRepository.StoreCommentCount::getStoreId,
                        Fp440CommentRepository.StoreCommentCount::getCnt
                ));

        // ✅ (B) 업로더 프로필 이미지 배치 조회 (username -> profileImageUrl)
        List<String> uploaderUsernames = resultStores.stream()
                .map(Fp300Store::getUsername)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<String, String> profileImageMap = uploaderUsernames.isEmpty()
                ? Collections.emptyMap()
                : memberRepository.findByUsernameIn(uploaderUsernames).stream()
                .collect(Collectors.toMap(
                        Fp100User::getUsername,
                        Fp100User::getProfileImageUrl,
                        (a, b) -> a
                ));

        // ✅ (C) 좋아요 수 배치 + 내가 좋아요 여부 배치
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

                // ✅ 좋아요 주입
                .likeCount(likeCount)
                .likedByMe(likedByMe)
                .build();
    }
}
