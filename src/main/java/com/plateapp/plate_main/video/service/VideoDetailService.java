package com.plateapp.plate_main.video.service;

import com.plateapp.plate_main.block.repository.BlockRepository;
import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import com.plateapp.plate_main.common.s3.S3UploadService;
import com.plateapp.plate_main.friend.repository.Fp150FriendRepository;
import com.plateapp.plate_main.like.service.LikeService;
import com.plateapp.plate_main.report.repository.ReportRepository;
import com.plateapp.plate_main.user.entity.Fp100User;
import com.plateapp.plate_main.user.repository.MemberRepository;
import com.plateapp.plate_main.video.dto.VideoFeedItemDTO;
import com.plateapp.plate_main.video.entity.Fp300Store;
import com.plateapp.plate_main.video.repository.Fp300StoreRepository;
import com.plateapp.plate_main.video.repository.Fp440CommentRepository;
import com.plateapp.plate_main.video.service.ContentPlaceResolver.ResolvedPlace;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VideoDetailService {

    private static final String FLAG_Y = "Y";
    private static final String REPORT_TARGET_VIDEO = "video";
    private static final String REPORT_TARGET_USER = "user";

    private final Fp300StoreRepository storeRepository;
    private final Fp440CommentRepository commentRepository;
    private final MemberRepository memberRepository;
    private final BlockRepository blockRepository;
    private final Fp150FriendRepository friendRepository;
    private final ReportRepository reportRepository;
    private final LikeService likeService;
    private final S3UploadService s3UploadService;
    private final ContentPlaceResolver contentPlaceResolver;
    private final VideoPlaybackUrlService videoPlaybackUrlService;

    @Transactional(readOnly = true)
    public VideoFeedItemDTO getVideo(String viewerUsername, Integer videoStoreId) {
        if (viewerUsername == null || viewerUsername.isBlank()) {
            throw new AppException(ErrorCode.AUTH_UNAUTHORIZED);
        }
        if (videoStoreId == null) {
            throw videoNotFound();
        }

        Fp300Store store = storeRepository.findById(videoStoreId)
                .orElseThrow(this::videoNotFound);
        String authorUsername = trimToNull(store.getUsername());
        String storedFileName = trimToNull(store.getFileName());

        if (!FLAG_Y.equalsIgnoreCase(trimToNull(store.getUseYn()))
                || store.getDeletedAt() != null
                || authorUsername == null
                || storedFileName == null) {
            throw videoNotFound();
        }

        Fp100User author = memberRepository.findById(authorUsername)
                .orElseThrow(this::videoNotFound);

        boolean owner = viewerUsername.equals(authorUsername);
        boolean publicVideo = FLAG_Y.equalsIgnoreCase(trimToNull(store.getOpenYn()));
        if (!owner && !publicVideo) {
            throw videoNotFound();
        }
        if (!owner
                && !Boolean.FALSE.equals(author.getIsPrivate())
                && !friendRepository.existsAcceptedRelationship(viewerUsername, authorUsername)) {
            throw videoNotFound();
        }
        if (isBlockedInEitherDirection(viewerUsername, authorUsername)
                || hasActiveViewerReport(viewerUsername, videoStoreId, author)) {
            throw videoNotFound();
        }

        boolean publiclyAccessibleMedia = publicVideo && Boolean.FALSE.equals(author.getIsPrivate());
        String playbackUrl = trimToNull(
                videoPlaybackUrlService.resolvePlaybackUrl(storedFileName, publiclyAccessibleMedia)
        );
        if (playbackUrl == null) {
            throw videoNotFound();
        }

        ResolvedPlace place = contentPlaceResolver.resolveDirect(
                store.getPlaceId(),
                store.getAddress()
        );
        Double lat = place.lat();
        Double lng = place.lng();
        if (lat == null || lng == null) {
            lat = null;
            lng = null;
        }

        long commentCount = activeCommentCount(videoStoreId);
        long likeCount = Math.max(0L, likeService.countLikes(videoStoreId));
        boolean likedByMe = likeService.isLiked(viewerUsername, videoStoreId);
        String profileImageUrl = trimToNull(author.getProfileImageUrl());

        return VideoFeedItemDTO.builder()
                .storeId(store.getStoreId())
                .placeId(trimToNull(place.placeId()))
                .title(trimToNull(store.getTitle()))
                .storeName(trimToNull(store.getStoreName()))
                .address(trimToNull(store.getAddress()))
                .lat(lat)
                .lng(lng)
                .fileName(playbackUrl)
                .thumbnail(toNullableImageUrl(store.getThumbnail()))
                .videoDuration(nonNegativeOrNull(store.getVideoDuration()))
                .createdAt(store.getCreatedAt())
                .commentCount(commentCount)
                .profileImageUrl(profileImageUrl)
                .username(authorUsername)
                .likeCount(likeCount)
                .likedByMe(likedByMe)
                .build();
    }

    private boolean isBlockedInEitherDirection(String viewerUsername, String authorUsername) {
        if (viewerUsername.equals(authorUsername)) {
            return false;
        }
        return blockRepository.existsByBlockerUsernameAndBlockedUsername(viewerUsername, authorUsername)
                || blockRepository.existsByBlockerUsernameAndBlockedUsername(authorUsername, viewerUsername);
    }

    private boolean hasActiveViewerReport(
            String viewerUsername,
            Integer videoStoreId,
            Fp100User author
    ) {
        boolean reportedVideo = reportRepository
                .existsByReporterUsernameAndTargetTypeIgnoreCaseAndTargetIdAndTargetFlagAndUnflaggedAtIsNull(
                        viewerUsername,
                        REPORT_TARGET_VIDEO,
                        videoStoreId,
                        FLAG_Y
                );
        if (reportedVideo) {
            return true;
        }
        return reportRepository.existsActiveUserReport(
                        viewerUsername,
                        author.getUsername(),
                        author.getUserId(),
                        REPORT_TARGET_USER,
                        FLAG_Y
                );
    }

    private long activeCommentCount(Integer videoStoreId) {
        List<Fp440CommentRepository.StoreCommentCount> rows =
                commentRepository.countActiveByStoreIds(List.of(videoStoreId));
        if (rows == null || rows.isEmpty()) {
            return 0L;
        }
        return rows.stream()
                .filter(java.util.Objects::nonNull)
                .filter(row -> videoStoreId.equals(row.getStoreId()))
                .map(Fp440CommentRepository.StoreCommentCount::getCnt)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .map(count -> Math.max(0L, count))
                .orElse(0L);
    }

    private String toNullableImageUrl(String storedPath) {
        String value = trimToNull(storedPath);
        return value == null ? null : trimToNull(s3UploadService.toImageUrl(value));
    }

    private Integer nonNegativeOrNull(Integer value) {
        return value == null || value < 0 ? null : value;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private AppException videoNotFound() {
        return new AppException(ErrorCode.VIDEO_NOT_FOUND);
    }
}
