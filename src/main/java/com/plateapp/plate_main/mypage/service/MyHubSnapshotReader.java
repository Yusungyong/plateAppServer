package com.plateapp.plate_main.mypage.service;

import com.plateapp.plate_main.auth.domain.User;
import com.plateapp.plate_main.auth.repository.UserRepository;
import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import com.plateapp.plate_main.common.s3.S3UploadService;
import com.plateapp.plate_main.mypage.dto.MyHubResponse;
import com.plateapp.plate_main.mypage.dto.MyHubResponse.Author;
import com.plateapp.plate_main.mypage.dto.MyHubResponse.ContentPreview;
import com.plateapp.plate_main.mypage.dto.MyHubResponse.ContentType;
import com.plateapp.plate_main.mypage.dto.MyHubResponse.Counts;
import com.plateapp.plate_main.mypage.dto.MyHubResponse.PrimaryAction;
import com.plateapp.plate_main.mypage.dto.MyHubResponse.Profile;
import com.plateapp.plate_main.mypage.dto.MyHubResponse.Section;
import com.plateapp.plate_main.mypage.dto.MyHubResponse.Store;
import com.plateapp.plate_main.mypage.dto.MyHubResponse.TimePrecision;
import com.plateapp.plate_main.mypage.repository.MyHubQueryRepository;
import com.plateapp.plate_main.mypage.repository.MyHubQueryRepository.ContentRow;
import com.plateapp.plate_main.mypage.repository.MyHubQueryRepository.CountsRow;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class MyHubSnapshotReader {

    private static final Logger log = LoggerFactory.getLogger(MyHubSnapshotReader.class);

    private final UserRepository userRepository;
    private final MyHubQueryRepository queryRepository;
    private final S3UploadService s3UploadService;

    public MyHubSnapshotReader(
            UserRepository userRepository,
            MyHubQueryRepository queryRepository,
            S3UploadService s3UploadService
    ) {
        this.userRepository = userRepository;
        this.queryRepository = queryRepository;
        this.s3UploadService = s3UploadService;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public MyHubResponse read(String username, int previewLimit) {
        User user = userRepository.findById(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        if (user.getIsPrivate() == null) {
            log.error("Cannot build my hub while account privacy is null");
            throw new AppException(ErrorCode.COMMON_INTERNAL_ERROR);
        }

        CountsRow row = queryRepository.findCounts(username);
        long contentCount;
        try {
            contentCount = Math.addExact(row.videoCount(), row.imageCount());
        } catch (ArithmeticException e) {
            log.error("My hub content count overflow", e);
            throw new AppException(ErrorCode.COMMON_INTERNAL_ERROR);
        }

        Counts counts = new Counts(
                contentCount,
                row.videoCount(),
                row.imageCount(),
                row.likedContentCount(),
                row.receivedLikeCount(),
                row.friendCount(),
                row.pendingFriendRequestCount()
        );

        List<Section> sections = availableSections(counts);
        List<ContentPreview> recentContent = previewLimit == 0 || contentCount == 0
                ? List.of()
                : mapContent(queryRepository.findRecentContent(username, previewLimit), false);
        List<ContentPreview> recentLikes = previewLimit == 0 || row.likedContentCount() == 0
                ? List.of()
                : mapContent(queryRepository.findRecentLikes(username, previewLimit), true);

        String activeRegion = trimToNull(user.getActiveRegion());
        Profile profile = new Profile(
                user.getUsername(),
                displayName(user.getNickname(), user.getUsername()),
                trimToNull(user.getProfileImageUrl()),
                activeRegion,
                user.getIsPrivate()
        );

        return new MyHubResponse(
                profile,
                counts,
                sections,
                recentContent,
                recentLikes,
                primaryAction(counts, activeRegion),
                Instant.now()
        );
    }

    private List<Section> availableSections(Counts counts) {
        List<Section> sections = new ArrayList<>(2);
        if (counts.contentCount() > 0) {
            sections.add(Section.RECENT_CONTENT);
        }
        if (counts.likedContentCount() > 0) {
            sections.add(Section.LIKED_CONTENT);
        }
        return List.copyOf(sections);
    }

    private PrimaryAction primaryAction(Counts counts, String activeRegion) {
        if (counts.contentCount() == 0 && counts.likedContentCount() == 0) {
            return PrimaryAction.EXPLORE_CONTENT;
        }
        if (activeRegion == null) {
            return PrimaryAction.SET_ACTIVE_REGION;
        }
        if (counts.contentCount() == 0) {
            return PrimaryAction.CREATE_CONTENT;
        }
        if (counts.friendCount() == 0) {
            return PrimaryAction.FIND_FRIENDS;
        }
        return null;
    }

    private List<ContentPreview> mapContent(List<ContentRow> rows, boolean likedPreview) {
        List<ContentPreview> items = new ArrayList<>(rows.size());
        for (ContentRow row : rows) {
            ContentPreview item = toContentPreview(row, likedPreview);
            if (item != null) {
                items.add(item);
            }
        }
        if (items.size() != rows.size()) {
            log.warn("Excluded {} malformed my hub preview rows", rows.size() - items.size());
        }
        return List.copyOf(items);
    }

    private ContentPreview toContentPreview(ContentRow row, boolean likedPreview) {
        ContentType contentType = parseContentType(row.contentType());
        if (contentType == null
                || row.sourceId() == null
                || trimToNull(row.authorUsername()) == null
                || row.createdOn() == null
                || (likedPreview && row.likedOn() == null)) {
            return null;
        }

        String placeId = trimToNull(row.placeId());
        String storeName = trimToNull(row.storeName());
        String address = trimToNull(row.address());
        Double latitude = row.latitude();
        Double longitude = row.longitude();
        if (latitude == null || longitude == null) {
            latitude = null;
            longitude = null;
        }

        Store store = placeId == null && storeName == null && address == null && latitude == null
                ? null
                : new Store(placeId, storeName, address, latitude, longitude);
        String title = trimToNull(row.title());
        if (title == null) {
            title = storeName;
        }

        return new ContentPreview(
                contentType,
                contentId(contentType, row.sourceId()),
                contentType == ContentType.VIDEO ? row.sourceId() : null,
                contentType == ContentType.IMAGE ? row.sourceId() : null,
                placeId,
                title,
                thumbnailUrl(contentType, row),
                store,
                new Author(
                        row.authorUsername(),
                        displayName(row.authorDisplayName(), row.authorUsername()),
                        trimToNull(row.authorProfileImageUrl())
                ),
                null,
                row.createdOn(),
                TimePrecision.DATE,
                null,
                likedPreview ? row.likedOn() : null,
                likedPreview ? TimePrecision.DATE : null
        );
    }

    private ContentType parseContentType(String value) {
        if (value == null) {
            return null;
        }
        try {
            return ContentType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String contentId(ContentType contentType, Integer sourceId) {
        return contentType.name().toLowerCase(Locale.ROOT) + ":" + sourceId;
    }

    private String thumbnailUrl(ContentType contentType, ContentRow row) {
        String source = trimToNull(row.thumbnailSource());
        if (source == null && contentType == ContentType.IMAGE) {
            source = firstImage(row.imagesSource());
        }
        if (source == null) {
            return null;
        }
        return contentType == ContentType.VIDEO
                ? trimToNull(s3UploadService.toImageUrl(source))
                : trimToNull(s3UploadService.toFeedImageUrl(source));
    }

    private String firstImage(String images) {
        String value = trimToNull(images);
        if (value == null) {
            return null;
        }
        int comma = value.indexOf(',');
        return trimToNull(comma >= 0 ? value.substring(0, comma) : value);
    }

    private String displayName(String value, String username) {
        return Objects.requireNonNullElse(trimToNull(value), username);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
