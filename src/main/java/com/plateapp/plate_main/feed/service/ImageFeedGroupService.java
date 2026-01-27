package com.plateapp.plate_main.feed.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;

import com.plateapp.plate_main.feed.dto.ImageFeedGroupImagesResponse;
import com.plateapp.plate_main.feed.dto.ImageFeedGroupResponse;
import com.plateapp.plate_main.feed.entity.Fp400ImageFeed;
import com.plateapp.plate_main.feed.repository.ImageFeedGroupQueryRepository;
import com.plateapp.plate_main.feed.repository.ImageFeedRepository;
import com.plateapp.plate_main.user.entity.Fp100User;

@Service
public class ImageFeedGroupService {

    private static final DateTimeFormatter CURSOR_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final ImageFeedGroupQueryRepository groupQueryRepository;
    private final ImageFeedRepository imageFeedRepository;

    public ImageFeedGroupService(
            ImageFeedGroupQueryRepository groupQueryRepository,
            ImageFeedRepository imageFeedRepository
    ) {
        this.groupQueryRepository = groupQueryRepository;
        this.imageFeedRepository = imageFeedRepository;
    }

    @Transactional(readOnly = true)
    public ImageFeedGroupResponse getGroups(
            Integer limit,
            String cursor,
            String sort,
            Double lat,
            Double lng,
            Integer radiusM
    ) {
        int safeLimit = clamp(limit, 1, 50, 20);
        Cursor cursorValue = parseCursor(cursor);
        String sortType = (sort == null || sort.isBlank()) ? "RECENT" : sort.toUpperCase();

        List<ImageFeedGroupQueryRepository.GroupRow> rows;
        if ("NEARBY".equals(sortType)) {
            if (lat == null || lng == null) {
                throw new IllegalArgumentException("lat and lng are required for NEARBY sort");
            }
            int safeRadius = radiusM == null ? 3000 : Math.max(100, Math.min(radiusM, 20000));
            double latDelta = safeRadius / 111320.0;
            double lngDelta = safeRadius / (111320.0 * Math.max(Math.cos(Math.toRadians(lat)), 0.01));
            rows = groupQueryRepository.findNearbyGroups(
                    lat,
                    lng,
                    latDelta,
                    lngDelta,
                    safeRadius,
                    cursorValue.createdAt,
                    cursorValue.feedId,
                    safeLimit
            );
        } else {
            rows = groupQueryRepository.findRecentGroups(
                    cursorValue.createdAt,
                    cursorValue.feedId,
                    safeLimit
            );
        }

        List<ImageFeedGroupResponse.GroupItem> items = rows.stream()
                .map(r -> new ImageFeedGroupResponse.GroupItem(
                        r.groupId(),
                        r.placeId(),
                        r.storeName(),
                        r.address(),
                        r.thumbnail(),
                        r.imageCount(),
                        r.latestFeedId(),
                        r.latestCreatedAt()
                ))
                .toList();

        String nextCursor = null;
        boolean hasMore = items.size() == safeLimit;
        if (hasMore) {
            ImageFeedGroupResponse.GroupItem last = items.get(items.size() - 1);
            if (last.latestCreatedAt() != null && last.latestFeedId() != null) {
                nextCursor = formatCursor(last.latestCreatedAt(), last.latestFeedId());
            }
        }

        return new ImageFeedGroupResponse(items, nextCursor, hasMore);
    }

    @Transactional(readOnly = true)
    public ImageFeedGroupImagesResponse getGroupImages(
            String groupId,
            Integer limit,
            String cursor
    ) {
        GroupKey key = parseGroupId(groupId);
        int safeLimit = clamp(limit, 1, 50, 5);
        Cursor cursorValue = parseCursor(cursor);

        List<Fp400ImageFeed> feeds = imageFeedRepository.findGroupFeeds(
                key.placeId,
                key.storeName,
                cursorValue.createdAt,
                cursorValue.feedId,
                PageRequest.of(0, safeLimit)
        );

        List<ImageFeedGroupImagesResponse.ImageItem> items = feeds.stream()
                .map(feed -> new ImageFeedGroupImagesResponse.ImageItem(
                        feed.getFeedId(),
                        firstImageOrThumbnail(feed),
                        feed.getCreatedAt(),
                        feed.getUsername(),
                        resolveNickname(feed.getWriter())
                ))
                .toList();

        String nextCursor = null;
        boolean hasMore = items.size() == safeLimit;
        if (hasMore) {
            ImageFeedGroupImagesResponse.ImageItem last = items.get(items.size() - 1);
            if (last.createdAt() != null && last.feedId() != null) {
                nextCursor = formatCursor(last.createdAt(), last.feedId());
            }
        }

        return new ImageFeedGroupImagesResponse(items, nextCursor, hasMore);
    }

    private String firstImageOrThumbnail(Fp400ImageFeed feed) {
        String images = feed.getImages();
        if (images != null && !images.isBlank()) {
            int idx = images.indexOf(',');
            return idx >= 0 ? images.substring(0, idx).trim() : images.trim();
        }
        return feed.getThumbnail();
    }

    private String resolveNickname(Fp100User writer) {
        if (writer == null) return null;
        String nick = writer.getNickName();
        if (nick != null && !nick.isBlank()) {
            return nick;
        }
        return writer.getUsername();
    }

    private Cursor parseCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return new Cursor(null, null);
        }
        String[] parts = cursor.split("\\|");
        if (parts.length != 2) {
            throw new IllegalArgumentException("invalid cursor");
        }
        LocalDateTime createdAt = LocalDateTime.parse(parts[0], CURSOR_FORMAT);
        Integer feedId = Integer.valueOf(parts[1]);
        return new Cursor(createdAt, feedId);
    }

    private String formatCursor(LocalDateTime createdAt, Integer feedId) {
        return CURSOR_FORMAT.format(createdAt) + "|" + feedId;
    }

    private GroupKey parseGroupId(String groupId) {
        if (groupId == null || groupId.isBlank()) {
            throw new IllegalArgumentException("groupId is required");
        }
        if (groupId.startsWith("place:")) {
            String placeId = groupId.substring("place:".length());
            if (placeId.isBlank()) {
                throw new IllegalArgumentException("invalid groupId");
            }
            return new GroupKey(placeId, null);
        }
        if (groupId.startsWith("store:")) {
            String storeName = groupId.substring("store:".length());
            if (storeName.isBlank()) {
                throw new IllegalArgumentException("invalid groupId");
            }
            return new GroupKey(null, storeName);
        }
        throw new IllegalArgumentException("invalid groupId");
    }

    private int clamp(Integer value, int min, int max, int fallback) {
        if (value == null) return fallback;
        return Math.min(Math.max(value, min), max);
    }

    private record Cursor(LocalDateTime createdAt, Integer feedId) {}

    private record GroupKey(String placeId, String storeName) {}
}
