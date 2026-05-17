package com.plateapp.plate_main.home.service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plateapp.plate_main.block.repository.BlockRepository;
import com.plateapp.plate_main.comment.repository.FeedCommentRepository;
import com.plateapp.plate_main.comment.repository.ReplyRepository;
import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import com.plateapp.plate_main.common.s3.S3UploadService;
import com.plateapp.plate_main.feed.entity.Fp400Feed;
import com.plateapp.plate_main.feed.repository.Fp400FeedRepository;
import com.plateapp.plate_main.home.dto.HomeContentAuthor;
import com.plateapp.plate_main.home.dto.HomeContentFeedItem;
import com.plateapp.plate_main.home.dto.HomeContentFeedResponse;
import com.plateapp.plate_main.home.dto.HomeContentPrimaryImage;
import com.plateapp.plate_main.home.dto.HomeContentStats;
import com.plateapp.plate_main.like.repository.FeedLikeRepository;
import com.plateapp.plate_main.like.service.LikeService;
import com.plateapp.plate_main.report.repository.ReportRepository;
import com.plateapp.plate_main.user.entity.Fp100User;
import com.plateapp.plate_main.user.repository.MemberRepository;
import com.plateapp.plate_main.video.entity.Fp300Store;
import com.plateapp.plate_main.video.repository.Fp300StoreRepository;
import com.plateapp.plate_main.video.repository.Fp440CommentRepository;

@Service
public class HomeContentFeedService {

    private static final int LIMIT_MAX = 30;
    private static final int CANDIDATE_MULTIPLIER = 4;
    private static final String TYPE_VIDEO = "VIDEO";
    private static final String TYPE_IMAGE = "IMAGE";

    private final Fp300StoreRepository videoRepository;
    private final Fp400FeedRepository imageRepository;
    private final MemberRepository memberRepository;
    private final LikeService likeService;
    private final FeedLikeRepository feedLikeRepository;
    private final Fp440CommentRepository videoCommentRepository;
    private final FeedCommentRepository feedCommentRepository;
    private final ReplyRepository replyRepository;
    private final BlockRepository blockRepository;
    private final ReportRepository reportRepository;
    private final S3UploadService s3UploadService;

    public HomeContentFeedService(
            Fp300StoreRepository videoRepository,
            Fp400FeedRepository imageRepository,
            MemberRepository memberRepository,
            LikeService likeService,
            FeedLikeRepository feedLikeRepository,
            Fp440CommentRepository videoCommentRepository,
            FeedCommentRepository feedCommentRepository,
            ReplyRepository replyRepository,
            BlockRepository blockRepository,
            ReportRepository reportRepository,
            S3UploadService s3UploadService
    ) {
        this.videoRepository = videoRepository;
        this.imageRepository = imageRepository;
        this.memberRepository = memberRepository;
        this.likeService = likeService;
        this.feedLikeRepository = feedLikeRepository;
        this.videoCommentRepository = videoCommentRepository;
        this.feedCommentRepository = feedCommentRepository;
        this.replyRepository = replyRepository;
        this.blockRepository = blockRepository;
        this.reportRepository = reportRepository;
        this.s3UploadService = s3UploadService;
    }

    @Transactional(readOnly = true)
    public HomeContentFeedResponse getContentFeed(
            String cursor,
            int limit,
            String surface,
            String username,
            boolean isGuest,
            String guestId,
            Double lat,
            Double lng,
            Double radius
    ) {
        int safeLimit = Math.min(Math.max(limit, 1), LIMIT_MAX);
        int offset = decodeOffset(cursor);
        int candidateSize = Math.max((offset + safeLimit) * CANDIDATE_MULTIPLIER, safeLimit * CANDIDATE_MULTIPLIER);

        String actorUsername = isGuest ? null : blankToNull(username);
        Set<String> excludedUsernames = loadExcludedUsernames(actorUsername);

        List<Fp300Store> videos = videoRepository.findLatestForHome(PageRequest.of(0, candidateSize));
        List<Fp400Feed> images = imageRepository.findLatestForHomeByGroup(null, null, PageRequest.of(0, candidateSize));

        videos = videos.stream()
                .filter(video -> video.getUsername() == null || !excludedUsernames.contains(video.getUsername()))
                .toList();
        images = images.stream()
                .filter(image -> image.getUsername() == null || !excludedUsernames.contains(image.getUsername()))
                .toList();

        FeedContext context = loadContext(videos, images, actorUsername);

        List<ScoredContent> candidates = new ArrayList<>();
        for (Fp300Store video : videos) {
            candidates.add(new ScoredContent(toVideoItem(video, context), scoreVideo(video, context)));
        }
        for (Fp400Feed image : images) {
            candidates.add(new ScoredContent(toImageItem(image, context), scoreImage(image, context)));
        }

        List<HomeContentFeedItem> ranked = diversify(candidates).stream()
                .map(ScoredContent::item)
                .toList();

        int end = Math.min(offset + safeLimit, ranked.size());
        List<HomeContentFeedItem> items = offset >= ranked.size()
                ? List.of()
                : ranked.subList(offset, end);
        String nextCursor = end < ranked.size() ? encodeOffset(end) : null;

        return new HomeContentFeedResponse(
                "home-content-" + UUID.randomUUID(),
                LocalDateTime.now(),
                items,
                nextCursor
        );
    }

    @Transactional(readOnly = true)
    public HomeContentFeedResponse searchContentFeed(
            String keyword,
            String cursor,
            int limit,
            String surface,
            String username,
            boolean isGuest,
            String guestId,
            Double lat,
            Double lng,
            Double radius
    ) {
        if (!hasText(keyword)) {
            return getContentFeed(cursor, limit, surface, username, isGuest, guestId, lat, lng, radius);
        }

        int safeLimit = Math.min(Math.max(limit, 1), LIMIT_MAX);
        int offset = decodeOffset(cursor);
        int candidateSize = Math.max((offset + safeLimit) * CANDIDATE_MULTIPLIER, 120);
        String normalizedKeyword = keyword.trim().toLowerCase();

        String actorUsername = isGuest ? null : blankToNull(username);
        Set<String> excludedUsernames = loadExcludedUsernames(actorUsername);

        List<Fp300Store> videos = videoRepository.findLatestForHome(PageRequest.of(0, candidateSize));
        List<Fp400Feed> images = imageRepository.findLatestForHomeByGroup(null, null, PageRequest.of(0, candidateSize));

        videos = videos.stream()
                .filter(video -> video.getUsername() == null || !excludedUsernames.contains(video.getUsername()))
                .filter(video -> matchesVideo(video, normalizedKeyword))
                .toList();
        images = images.stream()
                .filter(image -> image.getUsername() == null || !excludedUsernames.contains(image.getUsername()))
                .filter(image -> matchesImage(image, normalizedKeyword))
                .toList();

        FeedContext context = loadContext(videos, images, actorUsername);

        List<ScoredContent> candidates = new ArrayList<>();
        for (Fp300Store video : videos) {
            candidates.add(new ScoredContent(toVideoItem(video, context), scoreVideo(video, context)));
        }
        for (Fp400Feed image : images) {
            candidates.add(new ScoredContent(toImageItem(image, context), scoreImage(image, context)));
        }

        List<HomeContentFeedItem> ranked = diversify(candidates).stream()
                .map(ScoredContent::item)
                .filter(item -> matchesItem(item, normalizedKeyword))
                .toList();

        int end = Math.min(offset + safeLimit, ranked.size());
        List<HomeContentFeedItem> items = offset >= ranked.size()
                ? List.of()
                : ranked.subList(offset, end);
        String nextCursor = end < ranked.size() ? encodeOffset(end) : null;

        return new HomeContentFeedResponse(
                "home-content-search-" + UUID.randomUUID(),
                LocalDateTime.now(),
                items,
                nextCursor
        );
    }

    private FeedContext loadContext(List<Fp300Store> videos, List<Fp400Feed> images, String username) {
        List<Integer> videoIds = videos.stream()
                .map(Fp300Store::getStoreId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<Integer> imageIds = images.stream()
                .map(Fp400Feed::getFeedNo)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Set<String> usernames = new HashSet<>();
        videos.stream().map(Fp300Store::getUsername).filter(Objects::nonNull).forEach(usernames::add);
        images.stream().map(Fp400Feed::getUsername).filter(Objects::nonNull).forEach(usernames::add);

        Map<String, Fp100User> authorMap = usernames.isEmpty()
                ? Map.of()
                : memberRepository.findByUsernameIn(usernames).stream()
                        .collect(Collectors.toMap(Fp100User::getUsername, Function.identity(), (left, right) -> left));

        Map<Integer, Long> videoLikeCounts = defaultMap(likeService.getLikeCountMap(videoIds));
        Map<Integer, Long> videoCommentCounts = loadVideoCommentCounts(videoIds);
        Set<Integer> likedVideoIds = username == null || videoIds.isEmpty()
                ? Set.of()
                : likeService.getMyLikedStoreIdSet(username, videoIds);

        Map<Integer, Long> imageLikeCounts = loadImageLikeCounts(imageIds);
        Map<Integer, Long> imageCommentCounts = loadImageCommentCounts(imageIds);
        Set<Integer> likedImageIds = username == null || imageIds.isEmpty()
                ? Set.of()
                : new HashSet<>(feedLikeRepository.findMyActiveLikedFeedIds(username, imageIds));

        return new FeedContext(
                authorMap,
                videoLikeCounts,
                videoCommentCounts,
                likedVideoIds,
                imageLikeCounts,
                imageCommentCounts,
                likedImageIds
        );
    }

    private HomeContentFeedItem toVideoItem(Fp300Store video, FeedContext context) {
        Integer storeId = video.getStoreId();
        String title = hasText(video.getTitle()) ? video.getTitle() : video.getStoreName();

        return new HomeContentFeedItem(
                "video:" + storeId,
                TYPE_VIDEO,
                storeId,
                null,
                storeId,
                video.getPlaceId(),
                title,
                null,
                video.getStoreName(),
                video.getAddress(),
                s3UploadService.toImageUrl(video.getThumbnail()),
                s3UploadService.toVideoUrl(video.getFileName()),
                0.5625,
                video.getVideoDuration(),
                null,
                null,
                author(video.getUsername(), context),
                new HomeContentStats(
                        context.videoLikeCounts().getOrDefault(storeId, 0L),
                        context.videoCommentCounts().getOrDefault(storeId, 0L),
                        0L,
                        context.likedVideoIds().contains(storeId)
                ),
                "fresh_video",
                null,
                toDateTime(video.getCreatedAt())
        );
    }

    private HomeContentFeedItem toImageItem(Fp400Feed image, FeedContext context) {
        Integer feedId = image.getFeedNo();
        List<String> imagePaths = parseImages(image.getImages());
        String primaryPath = imagePaths.isEmpty() ? image.getThumbnail() : imagePaths.get(0);
        HomeContentPrimaryImage primaryImage = primaryPath == null
                ? null
                : new HomeContentPrimaryImage(
                        1,
                        s3UploadService.toFeedImageUrl(buildThumbnailRelativePath(primaryPath)),
                        s3UploadService.toFeedImageUrl(primaryPath),
                        null
                );

        return new HomeContentFeedItem(
                "image:" + feedId,
                TYPE_IMAGE,
                null,
                feedId,
                null,
                image.getPlaceId(),
                firstText(image.getFeedTitle(), image.getContent()),
                image.getContent(),
                image.getStoreName(),
                image.getLocation(),
                primaryImage != null ? primaryImage.thumbnailUrl() : null,
                null,
                null,
                null,
                imagePaths.size(),
                primaryImage,
                author(image.getUsername(), context),
                new HomeContentStats(
                        context.imageLikeCounts().getOrDefault(feedId, 0L),
                        context.imageCommentCounts().getOrDefault(feedId, 0L),
                        0L,
                        context.likedImageIds().contains(feedId)
                ),
                "fresh_image_explore",
                null,
                image.getCreatedAt()
        );
    }

    private double scoreVideo(Fp300Store video, FeedContext context) {
        Integer id = video.getStoreId();
        double score = recencyScore(toDateTime(video.getCreatedAt()), toDateTime(video.getUpdatedAt()));
        score += Math.min(context.videoLikeCounts().getOrDefault(id, 0L) * 0.18, 3.0);
        score += Math.min(context.videoCommentCounts().getOrDefault(id, 0L) * 0.22, 3.0);
        if (hasText(video.getThumbnail())) score += 0.2;
        if (hasText(video.getPlaceId())) score += 0.15;
        return score;
    }

    private double scoreImage(Fp400Feed image, FeedContext context) {
        Integer id = image.getFeedNo();
        double score = recencyScore(image.getCreatedAt(), image.getUpdatedAt());
        score += Math.min(context.imageLikeCounts().getOrDefault(id, 0L) * 0.18, 3.0);
        score += Math.min(context.imageCommentCounts().getOrDefault(id, 0L) * 0.22, 3.0);
        score += Math.min(parseImages(image.getImages()).size() * 0.18, 1.2);
        if (hasText(image.getPlaceId())) score += 0.15;
        return score;
    }

    private List<ScoredContent> diversify(List<ScoredContent> candidates) {
        List<ScoredContent> remaining = candidates.stream()
                .sorted(Comparator.comparingDouble(ScoredContent::score).reversed())
                .collect(Collectors.toCollection(ArrayList::new));
        List<ScoredContent> result = new ArrayList<>();

        while (!remaining.isEmpty()) {
            ScoredContent selected = null;
            for (ScoredContent candidate : remaining) {
                if (allowedNext(result, candidate.item())) {
                    selected = candidate;
                    break;
                }
            }
            if (selected == null) {
                selected = remaining.get(0);
            }
            result.add(selected.withScoreInItem());
            remaining.remove(selected);
        }

        return result;
    }

    private boolean allowedNext(List<ScoredContent> result, HomeContentFeedItem next) {
        if (result.isEmpty()) {
            return true;
        }
        HomeContentFeedItem prev = result.get(result.size() - 1).item();
        if (sameText(prev.author().username(), next.author().username())) {
            return false;
        }
        if (sameText(prev.placeId(), next.placeId()) && hasText(next.placeId())) {
            return false;
        }
        if (result.size() >= 3) {
            List<HomeContentFeedItem> tail = result.subList(result.size() - 3, result.size()).stream()
                    .map(ScoredContent::item)
                    .toList();
            boolean sameTypeTail = tail.stream().allMatch(item -> item.contentType().equals(next.contentType()));
            if (sameTypeTail) {
                return false;
            }
        }
        return true;
    }

    private HomeContentAuthor author(String username, FeedContext context) {
        Fp100User user = context.authorMap().get(username);
        return new HomeContentAuthor(
                user != null ? user.getUserId() : null,
                username,
                user != null ? user.getNickName() : null,
                user != null ? user.getProfileImageUrl() : null
        );
    }

    private Map<Integer, Long> loadVideoCommentCounts(List<Integer> storeIds) {
        if (storeIds.isEmpty()) return Map.of();
        return videoCommentRepository.countActiveByStoreIds(storeIds).stream()
                .collect(Collectors.toMap(
                        Fp440CommentRepository.StoreCommentCount::getStoreId,
                        Fp440CommentRepository.StoreCommentCount::getCnt
                ));
    }

    private Map<Integer, Long> loadImageLikeCounts(List<Integer> feedIds) {
        if (feedIds.isEmpty()) return Map.of();
        return feedLikeRepository.countActiveByFeedIds(feedIds).stream()
                .collect(Collectors.toMap(
                        FeedLikeRepository.FeedLikeCount::getFeedId,
                        FeedLikeRepository.FeedLikeCount::getCnt
                ));
    }

    private Map<Integer, Long> loadImageCommentCounts(List<Integer> feedIds) {
        if (feedIds.isEmpty()) return Map.of();
        Map<Integer, Long> counts = new HashMap<>();
        for (FeedCommentRepository.FeedCommentCount row : feedCommentRepository.countActiveByFeedIds(feedIds)) {
            counts.put(row.getFeedId(), row.getCnt());
        }
        for (ReplyRepository.FeedReplyCountRow row : replyRepository.countActiveByFeedIds(feedIds)) {
            counts.merge(row.getFeedId(), row.getCnt(), Long::sum);
        }
        return counts;
    }

    private Set<String> loadExcludedUsernames(String username) {
        if (!hasText(username)) {
            return Set.of();
        }
        Set<String> excluded = new HashSet<>();
        List<String> blocked = blockRepository.findBlockedUsernames(username);
        if (blocked != null) excluded.addAll(blocked);
        List<String> reported = reportRepository.findReportedUsernames(username);
        if (reported != null) excluded.addAll(reported);
        return excluded;
    }

    private double recencyScore(LocalDateTime createdAt, LocalDateTime updatedAt) {
        LocalDateTime reference = updatedAt != null ? updatedAt : createdAt;
        if (reference == null) return 0.0;
        long hours = Math.max(0, java.time.temporal.ChronoUnit.HOURS.between(reference, LocalDateTime.now()));
        return Math.max(0.5, 5.0 - Math.min(hours, 168) * 0.03);
    }

    private LocalDateTime toDateTime(LocalDate date) {
        return date == null ? null : date.atStartOfDay();
    }

    private List<String> parseImages(String images) {
        if (!hasText(images)) return List.of();
        return java.util.Arrays.stream(images.split(","))
                .map(String::trim)
                .filter(this::hasText)
                .distinct()
                .toList();
    }

    private String buildThumbnailRelativePath(String relativePath) {
        if (!hasText(relativePath)) return null;
        int slash = relativePath.indexOf('/');
        if (slash <= 0 || slash == relativePath.length() - 1) return relativePath;
        String datePrefix = relativePath.substring(0, slash);
        String filename = relativePath.substring(slash + 1);
        return datePrefix + "/thumbnails/300x300/" + filename;
    }

    private int decodeOffset(String cursor) {
        if (!hasText(cursor)) return 0;
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            if (!decoded.startsWith("offset:")) return 0;
            return Math.max(0, Integer.parseInt(decoded.substring("offset:".length())));
        } catch (RuntimeException e) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT);
        }
    }

    private String encodeOffset(int offset) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(("offset:" + offset).getBytes(StandardCharsets.UTF_8));
    }

    private String firstText(String first, String second) {
        return hasText(first) ? first : second;
    }

    private String blankToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private boolean sameText(String left, String right) {
        return hasText(left) && left.equals(right);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private boolean matchesVideo(Fp300Store video, String keyword) {
        return containsIgnoreCase(video.getStoreName(), keyword)
                || containsIgnoreCase(video.getTitle(), keyword)
                || containsIgnoreCase(video.getAddress(), keyword)
                || containsIgnoreCase(video.getUsername(), keyword);
    }

    private boolean matchesImage(Fp400Feed image, String keyword) {
        return containsIgnoreCase(image.getStoreName(), keyword)
                || containsIgnoreCase(image.getFeedTitle(), keyword)
                || containsIgnoreCase(image.getContent(), keyword)
                || containsIgnoreCase(image.getLocation(), keyword)
                || containsIgnoreCase(image.getUsername(), keyword);
    }

    private boolean matchesItem(HomeContentFeedItem item, String keyword) {
        return containsIgnoreCase(item.title(), keyword)
                || containsIgnoreCase(item.content(), keyword)
                || containsIgnoreCase(item.storeName(), keyword)
                || containsIgnoreCase(item.address(), keyword)
                || containsIgnoreCase(item.author() != null ? item.author().username() : null, keyword)
                || containsIgnoreCase(item.author() != null ? item.author().nickName() : null, keyword);
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        return hasText(value) && value.toLowerCase().contains(keyword);
    }

    private <K, V> Map<K, V> defaultMap(Map<K, V> map) {
        return map != null ? map : Collections.emptyMap();
    }

    private record FeedContext(
            Map<String, Fp100User> authorMap,
            Map<Integer, Long> videoLikeCounts,
            Map<Integer, Long> videoCommentCounts,
            Set<Integer> likedVideoIds,
            Map<Integer, Long> imageLikeCounts,
            Map<Integer, Long> imageCommentCounts,
            Set<Integer> likedImageIds
    ) {
    }

    private record ScoredContent(HomeContentFeedItem item, double score) {
        ScoredContent withScoreInItem() {
            return new ScoredContent(new HomeContentFeedItem(
                    item.feedKey(),
                    item.contentType(),
                    item.videoFeedId(),
                    item.imageFeedId(),
                    item.storeId(),
                    item.placeId(),
                    item.title(),
                    item.content(),
                    item.storeName(),
                    item.address(),
                    item.thumbnailUrl(),
                    item.videoUrl(),
                    item.aspectRatio(),
                    item.durationSec(),
                    item.imageCount(),
                    item.primaryImage(),
                    item.author(),
                    item.stats(),
                    item.reason(),
                    Math.round(score * 1000.0) / 1000.0,
                    item.createdAt()
            ), score);
        }
    }
}
