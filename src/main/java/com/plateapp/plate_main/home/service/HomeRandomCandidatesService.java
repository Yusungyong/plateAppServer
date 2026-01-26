package com.plateapp.plate_main.home.service;

import com.plateapp.plate_main.feed.entity.Fp400Feed;
import com.plateapp.plate_main.feed.repository.Fp400FeedRepository;
import com.plateapp.plate_main.home.dto.HomeRandomCandidatesResponse;
import com.plateapp.plate_main.block.repository.BlockRepository;
import com.plateapp.plate_main.report.repository.ReportRepository;
import com.plateapp.plate_main.video.entity.Fp300Store;
import com.plateapp.plate_main.video.repository.Fp300StoreRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HomeRandomCandidatesService {

    private static final int LIMIT_MAX = 100;
    private static final int LIMIT_DEFAULT = 50;
    private static final double RADIUS_DEFAULT = 1500.0;
    private static final double RADIUS_MIN = 300.0;
    private static final double RADIUS_MAX = 5000.0;

    private final Fp300StoreRepository storeRepository;
    private final Fp400FeedRepository feedRepository;
    private final BlockRepository blockRepository;
    private final ReportRepository reportRepository;

    public HomeRandomCandidatesService(
            Fp300StoreRepository storeRepository,
            Fp400FeedRepository feedRepository,
            BlockRepository blockRepository,
            ReportRepository reportRepository
    ) {
        this.storeRepository = storeRepository;
        this.feedRepository = feedRepository;
        this.blockRepository = blockRepository;
        this.reportRepository = reportRepository;
    }

    @Transactional(readOnly = true)
    public HomeRandomCandidatesResponse getRecent(int limit, String include, String username) {
        int safeLimit = normalizeLimit(limit);
        IncludeType type = IncludeType.from(include);
        Set<String> excluded = loadExcludedUsernames(username);

        List<HomeRandomCandidatesResponse.Item> items = new ArrayList<>();
        if (type.includeVideo()) {
            List<Fp300Store> videos = storeRepository.findLatestForHome(PageRequest.of(0, safeLimit));
            for (Fp300Store store : videos) {
                if (!excluded.isEmpty() && store.getUsername() != null && excluded.contains(store.getUsername())) {
                    continue;
                }
                items.add(toVideoItem(store));
            }
        }
        if (type.includeImage()) {
            List<Fp400Feed> feeds = feedRepository.findLatestForHome(PageRequest.of(0, safeLimit));
            for (Fp400Feed feed : feeds) {
                if (!excluded.isEmpty() && feed.getUsername() != null && excluded.contains(feed.getUsername())) {
                    continue;
                }
                items.add(toImageItem(feed));
            }
        }

        items.sort(Comparator.comparing((HomeRandomCandidatesResponse.Item i) -> i.createdAt).reversed());
        if (items.size() > safeLimit) {
            items = items.subList(0, safeLimit);
        }
        return new HomeRandomCandidatesResponse(items);
    }

    @Transactional(readOnly = true)
    public HomeRandomCandidatesResponse getNearby(
            double lat,
            double lng,
            Double radius,
            int limit,
            String include,
            String username
    ) {
        int safeLimit = normalizeLimit(limit);
        double safeRadius = normalizeRadius(radius);
        IncludeType type = IncludeType.from(include);
        Set<String> excluded = loadExcludedUsernames(username);

        List<HomeRandomCandidatesResponse.Item> items = new ArrayList<>();
        if (type.includeVideo()) {
            List<Fp300Store> videos = storeRepository.findNearbyStores(
                    lat,
                    lng,
                    safeRadius,
                    null,
                    safeLimit
            );
            for (Fp300Store store : videos) {
                if (!excluded.isEmpty() && store.getUsername() != null && excluded.contains(store.getUsername())) {
                    continue;
                }
                items.add(toVideoItem(store));
            }
        }
        if (type.includeImage()) {
            List<Fp400Feed> feeds = feedRepository.findNearbyForHome(
                    lat,
                    lng,
                    safeRadius,
                    PageRequest.of(0, safeLimit)
            );
            for (Fp400Feed feed : feeds) {
                if (!excluded.isEmpty() && feed.getUsername() != null && excluded.contains(feed.getUsername())) {
                    continue;
                }
                items.add(toImageItem(feed));
            }
        }

        items.sort(Comparator.comparing((HomeRandomCandidatesResponse.Item i) -> i.createdAt).reversed());
        if (items.size() > safeLimit) {
            items = items.subList(0, safeLimit);
        }
        return new HomeRandomCandidatesResponse(items);
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return LIMIT_DEFAULT;
        }
        return Math.min(limit, LIMIT_MAX);
    }

    private double normalizeRadius(Double radius) {
        if (radius == null) {
            return RADIUS_DEFAULT;
        }
        if (radius < RADIUS_MIN) {
            return RADIUS_MIN;
        }
        if (radius > RADIUS_MAX) {
            return RADIUS_MAX;
        }
        return radius;
    }

    private HomeRandomCandidatesResponse.Item toVideoItem(Fp300Store store) {
        OffsetDateTime createdAt = toOffset(store.getCreatedAt(), store.getUpdatedAt());
        return new HomeRandomCandidatesResponse.Item(
                "video",
                store.getStoreId(),
                null,
                store.getPlaceId(),
                store.getStoreName(),
                store.getAddress(),
                store.getThumbnail(),
                createdAt
        );
    }

    private HomeRandomCandidatesResponse.Item toImageItem(Fp400Feed feed) {
        OffsetDateTime createdAt = toOffset(feed.getCreatedAt(), feed.getUpdatedAt());
        return new HomeRandomCandidatesResponse.Item(
                "image",
                null,
                feed.getFeedNo(),
                feed.getPlaceId(),
                feed.getStoreName(),
                feed.getLocation(),
                resolveFirstImage(feed.getImages()),
                createdAt
        );
    }

    private OffsetDateTime toOffset(LocalDate createdAt, LocalDate updatedAt) {
        LocalDate date = createdAt != null ? createdAt : updatedAt;
        if (date == null) {
            return OffsetDateTime.now(ZoneOffset.UTC);
        }
        return date.atStartOfDay().atOffset(ZoneOffset.UTC);
    }

    private OffsetDateTime toOffset(LocalDateTime createdAt, LocalDateTime updatedAt) {
        LocalDateTime dt = createdAt != null ? createdAt : updatedAt;
        if (dt == null) {
            return OffsetDateTime.now(ZoneOffset.UTC);
        }
        return dt.atOffset(ZoneOffset.UTC);
    }

    private String resolveFirstImage(String images) {
        if (images == null || images.isBlank()) {
            return null;
        }
        String[] arr = images.split(",");
        return arr.length > 0 ? arr[0].trim() : null;
    }

    private Set<String> loadExcludedUsernames(String username) {
        if (username == null || username.isBlank()) {
            return Set.of();
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

    private enum IncludeType {
        ALL,
        VIDEO,
        IMAGE;

        static IncludeType from(String include) {
            if ("video".equalsIgnoreCase(include)) {
                return VIDEO;
            }
            if ("image".equalsIgnoreCase(include)) {
                return IMAGE;
            }
            return ALL;
        }

        boolean includeVideo() {
            return this == ALL || this == VIDEO;
        }

        boolean includeImage() {
            return this == ALL || this == IMAGE;
        }
    }
}
