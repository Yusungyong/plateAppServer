package com.plateapp.plate_main.video.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VideoDetailServiceTest {

    @Mock
    private Fp300StoreRepository storeRepository;
    @Mock
    private Fp440CommentRepository commentRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private BlockRepository blockRepository;
    @Mock
    private Fp150FriendRepository friendRepository;
    @Mock
    private ReportRepository reportRepository;
    @Mock
    private LikeService likeService;
    @Mock
    private S3UploadService s3UploadService;
    @Mock
    private ContentPlaceResolver contentPlaceResolver;
    @Mock
    private VideoPlaybackUrlService videoPlaybackUrlService;

    @InjectMocks
    private VideoDetailService videoDetailService;

    @Test
    void returnsExactlyTheRequestedPublicVideoWithNullableLegacyFields() {
        Fp300Store store = activeStore(81, "author", "Y");
        store.setTitle("기존 영상");
        store.setStoreName("기존 매장명");
        store.setAddress("기존 주소");
        store.setCreatedAt(LocalDate.of(2025, 1, 3));
        store.setVideoDuration(18);
        stubReadableVideo(store, true, "https://media.example.com/video/81");
        when(contentPlaceResolver.resolveDirect(null, "기존 주소"))
                .thenReturn(new ContentPlaceResolver.ResolvedPlace(null, null, null, "기존 주소"));
        when(commentRepository.countActiveByStoreIds(List.of(81))).thenReturn(List.of());
        when(likeService.countLikes(81)).thenReturn(0L);
        when(likeService.isLiked("viewer", 81)).thenReturn(false);

        VideoFeedItemDTO result = videoDetailService.getVideo("viewer", 81);

        assertThat(result.getStoreId()).isEqualTo(81);
        assertThat(result.getPlaceId()).isNull();
        assertThat(result.getFileName()).isEqualTo("https://media.example.com/video/81");
        assertThat(result.getThumbnail()).isNull();
        assertThat(result.getProfileImageUrl()).isNull();
        assertThat(result.getLat()).isNull();
        assertThat(result.getLng()).isNull();
        assertThat(result.getCommentCount()).isZero();
        assertThat(result.getLikeCount()).isZero();
        assertThat(result.getLikedByMe()).isFalse();
        verify(reportRepository)
                .existsByReporterUsernameAndTargetTypeIgnoreCaseAndTargetIdAndTargetFlagAndUnflaggedAtIsNull(
                        "viewer", "video", 81, "Y");
    }

    @Test
    void ownerCanOpenPrivateVideoThroughPrivatePlaybackUrl() {
        Fp300Store store = activeStore(82, "owner", "N");
        stubReadableVideo(store, false, "https://signed.example.com/video/82");
        when(contentPlaceResolver.resolveDirect(null, null))
                .thenReturn(new ContentPlaceResolver.ResolvedPlace(null, null, null, null));
        when(commentRepository.countActiveByStoreIds(List.of(82))).thenReturn(List.of());

        VideoFeedItemDTO result = videoDetailService.getVideo("owner", 82);

        assertThat(result.getFileName()).isEqualTo("https://signed.example.com/video/82");
        verify(videoPlaybackUrlService).resolvePlaybackUrl("video.mp4", false);
        verify(blockRepository, never()).existsByBlockerUsernameAndBlockedUsername("owner", "owner");
    }

    @Test
    void nonOwnerCannotOpenPrivateVideo() {
        Fp300Store store = activeStore(83, "owner", "N");
        when(storeRepository.findById(83)).thenReturn(Optional.of(store));
        stubAuthor("owner", false);

        assertVideoNotFound(() -> videoDetailService.getVideo("viewer", 83));

        verify(videoPlaybackUrlService, never()).resolvePlaybackUrl("video.mp4", false);
    }

    @Test
    void missingInactiveDeletedOrFilelessVideoUsesSameNotFoundError() {
        when(storeRepository.findById(84)).thenReturn(Optional.empty());
        assertVideoNotFound(() -> videoDetailService.getVideo("viewer", 84));

        Fp300Store inactive = activeStore(85, "author", "Y");
        inactive.setUseYn("N");
        when(storeRepository.findById(85)).thenReturn(Optional.of(inactive));
        assertVideoNotFound(() -> videoDetailService.getVideo("viewer", 85));

        Fp300Store deleted = activeStore(86, "author", "Y");
        deleted.setDeletedAt(LocalDate.now());
        when(storeRepository.findById(86)).thenReturn(Optional.of(deleted));
        assertVideoNotFound(() -> videoDetailService.getVideo("viewer", 86));

        Fp300Store fileless = activeStore(87, "author", "Y");
        fileless.setFileName("  ");
        when(storeRepository.findById(87)).thenReturn(Optional.of(fileless));
        assertVideoNotFound(() -> videoDetailService.getVideo("viewer", 87));
    }

    @Test
    void blockInEitherDirectionHidesVideo() {
        Fp300Store viewerBlockedAuthor = activeStore(88, "author", "Y");
        when(storeRepository.findById(88)).thenReturn(Optional.of(viewerBlockedAuthor));
        stubAuthor("author", false);
        when(blockRepository.existsByBlockerUsernameAndBlockedUsername("viewer", "author"))
                .thenReturn(true);
        assertVideoNotFound(() -> videoDetailService.getVideo("viewer", 88));

        Fp300Store authorBlockedViewer = activeStore(89, "other-author", "Y");
        when(storeRepository.findById(89)).thenReturn(Optional.of(authorBlockedViewer));
        stubAuthor("other-author", false);
        when(blockRepository.existsByBlockerUsernameAndBlockedUsername("viewer", "other-author"))
                .thenReturn(false);
        when(blockRepository.existsByBlockerUsernameAndBlockedUsername("other-author", "viewer"))
                .thenReturn(true);
        assertVideoNotFound(() -> videoDetailService.getVideo("viewer", 89));
    }

    @Test
    void activeReportByCurrentViewerHidesOnlyThatVideo() {
        Fp300Store store = activeStore(90, "author", "Y");
        when(storeRepository.findById(90)).thenReturn(Optional.of(store));
        stubAuthor("author", false);
        when(reportRepository
                .existsByReporterUsernameAndTargetTypeIgnoreCaseAndTargetIdAndTargetFlagAndUnflaggedAtIsNull(
                        "viewer", "video", 90, "Y"))
                .thenReturn(true);

        assertVideoNotFound(() -> videoDetailService.getVideo("viewer", 90));

        verify(reportRepository)
                .existsByReporterUsernameAndTargetTypeIgnoreCaseAndTargetIdAndTargetFlagAndUnflaggedAtIsNull(
                        "viewer", "video", 90, "Y");
        verify(videoPlaybackUrlService, never()).resolvePlaybackUrl("video.mp4", true);
    }

    @Test
    void unresolvablePlaybackAndIncompleteCoordinatesDoNotLeakOrInventValues() {
        Fp300Store unresolvable = activeStore(91, "author", "Y");
        when(storeRepository.findById(91)).thenReturn(Optional.of(unresolvable));
        stubAuthor("author", false);
        when(videoPlaybackUrlService.resolvePlaybackUrl("video.mp4", true)).thenReturn(null);
        assertVideoNotFound(() -> videoDetailService.getVideo("viewer", 91));

        Fp300Store partialCoordinates = activeStore(92, "other-author", "Y");
        stubReadableVideo(partialCoordinates, true, "https://media.example.com/video/92");
        when(contentPlaceResolver.resolveDirect(null, null))
                .thenReturn(new ContentPlaceResolver.ResolvedPlace("place-92", 37.5, null, null));
        when(commentRepository.countActiveByStoreIds(List.of(92))).thenReturn(List.of());

        VideoFeedItemDTO result = videoDetailService.getVideo("viewer", 92);

        assertThat(result.getPlaceId()).isEqualTo("place-92");
        assertThat(result.getLat()).isNull();
        assertThat(result.getLng()).isNull();
    }

    @Test
    void missingAuthorAccountHidesOtherwiseActiveVideo() {
        Fp300Store store = activeStore(93, "missing-author", "Y");
        when(storeRepository.findById(93)).thenReturn(Optional.of(store));
        when(memberRepository.findById("missing-author")).thenReturn(Optional.empty());

        assertVideoNotFound(() -> videoDetailService.getVideo("viewer", 93));

        verify(videoPlaybackUrlService, never()).resolvePlaybackUrl("video.mp4", true);
    }

    @Test
    void privateOrUnknownAuthorPrivacyRequiresAcceptedRelationship() {
        Fp300Store privateAuthorVideo = activeStore(94, "private-author", "Y");
        when(storeRepository.findById(94)).thenReturn(Optional.of(privateAuthorVideo));
        stubAuthor("private-author", true);
        when(friendRepository.existsAcceptedRelationship("viewer", "private-author")).thenReturn(false);
        assertVideoNotFound(() -> videoDetailService.getVideo("viewer", 94));

        Fp300Store unknownPrivacyVideo = activeStore(95, "legacy-author", "Y");
        when(storeRepository.findById(95)).thenReturn(Optional.of(unknownPrivacyVideo));
        stubAuthor("legacy-author", null);
        when(friendRepository.existsAcceptedRelationship("viewer", "legacy-author")).thenReturn(false);
        assertVideoNotFound(() -> videoDetailService.getVideo("viewer", 95));
    }

    @Test
    void acceptedFriendCanOpenPublicVideoFromPrivateAccount() {
        Fp300Store store = activeStore(96, "private-author", "Y");
        stubReadableVideo(store, false, "https://signed.example.com/video/96");
        stubAuthor("private-author", true);
        when(friendRepository.existsAcceptedRelationship("viewer", "private-author")).thenReturn(true);
        when(contentPlaceResolver.resolveDirect(null, null))
                .thenReturn(new ContentPlaceResolver.ResolvedPlace(null, null, null, null));
        when(commentRepository.countActiveByStoreIds(List.of(96))).thenReturn(List.of());

        VideoFeedItemDTO result = videoDetailService.getVideo("viewer", 96);

        assertThat(result.getStoreId()).isEqualTo(96);
        assertThat(result.getFileName()).isEqualTo("https://signed.example.com/video/96");
        verify(friendRepository).existsAcceptedRelationship("viewer", "private-author");
        verify(videoPlaybackUrlService).resolvePlaybackUrl("video.mp4", false);
    }

    @Test
    void activeAuthorReportByCurrentViewerAlsoHidesVideo() {
        Fp300Store store = activeStore(97, "reported-author", "Y");
        when(storeRepository.findById(97)).thenReturn(Optional.of(store));
        stubAuthor("reported-author", false);
        when(reportRepository.existsActiveUserReport(
                        "viewer", "reported-author", null, "user", "Y"))
                .thenReturn(true);

        assertVideoNotFound(() -> videoDetailService.getVideo("viewer", 97));

        verify(videoPlaybackUrlService, never()).resolvePlaybackUrl("video.mp4", true);
    }

    @Test
    void activeAuthorReportByUserIdAlsoHidesVideoWhenUsernameWasNotStored() {
        Fp300Store store = activeStore(98, "reported-author", "Y");
        when(storeRepository.findById(98)).thenReturn(Optional.of(store));
        stubAuthor("reported-author", false, 321);
        when(reportRepository
                .existsByReporterUsernameAndTargetTypeIgnoreCaseAndTargetIdAndTargetFlagAndUnflaggedAtIsNull(
                        "viewer", "video", 98, "Y"))
                .thenReturn(false);
        when(reportRepository.existsActiveUserReport(
                        "viewer", "reported-author", 321, "user", "Y"))
                .thenReturn(true);

        assertVideoNotFound(() -> videoDetailService.getVideo("viewer", 98));

        verify(videoPlaybackUrlService, never()).resolvePlaybackUrl("video.mp4", true);
    }

    private Fp300Store activeStore(int storeId, String authorUsername, String openYn) {
        Fp300Store store = new Fp300Store();
        store.setStoreId(storeId);
        store.setUsername(authorUsername);
        store.setUseYn("Y");
        store.setOpenYn(openYn);
        store.setFileName("video.mp4");
        return store;
    }

    private void stubReadableVideo(Fp300Store store, boolean publicPlayback, String playbackUrl) {
        when(storeRepository.findById(store.getStoreId())).thenReturn(Optional.of(store));
        stubAuthor(store.getUsername(), false);
        when(videoPlaybackUrlService.resolvePlaybackUrl("video.mp4", publicPlayback))
                .thenReturn(playbackUrl);
    }

    private void stubAuthor(String username, Boolean isPrivate) {
        stubAuthor(username, isPrivate, null);
    }

    private void stubAuthor(String username, Boolean isPrivate, Integer userId) {
        Fp100User author = new Fp100User();
        author.setUsername(username);
        author.setIsPrivate(isPrivate);
        author.setUserId(userId);
        when(memberRepository.findById(username)).thenReturn(Optional.of(author));
    }

    private void assertVideoNotFound(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThatThrownBy(call)
                .isInstanceOfSatisfying(AppException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.VIDEO_NOT_FOUND));
    }
}
