package com.plateapp.plate_main.mypage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.plateapp.plate_main.auth.domain.User;
import com.plateapp.plate_main.auth.repository.UserRepository;
import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import com.plateapp.plate_main.common.s3.S3UploadService;
import com.plateapp.plate_main.mypage.config.MyHubProperties;
import com.plateapp.plate_main.mypage.dto.MyHubResponse;
import com.plateapp.plate_main.mypage.dto.MyHubResponse.ContentType;
import com.plateapp.plate_main.mypage.dto.MyHubResponse.PrimaryAction;
import com.plateapp.plate_main.mypage.dto.MyHubResponse.Section;
import com.plateapp.plate_main.mypage.dto.MyHubResponse.TimePrecision;
import com.plateapp.plate_main.mypage.repository.MyHubQueryRepository;
import com.plateapp.plate_main.mypage.repository.MyHubQueryRepository.ContentRow;
import com.plateapp.plate_main.mypage.repository.MyHubQueryRepository.CountsRow;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MyHubServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private MyHubQueryRepository queryRepository;
    @Mock
    private S3UploadService s3UploadService;

    private MyHubProperties properties;
    private MyHubService service;

    @BeforeEach
    void setUp() {
        properties = new MyHubProperties();
        properties.setEnabled(true);
        properties.setImageVisibilityReady(true);
        MyHubSnapshotReader snapshotReader = new MyHubSnapshotReader(
                userRepository,
                queryRepository,
                s3UploadService
        );
        service = new MyHubService(properties, snapshotReader);
    }

    @Test
    void disabledFeatureDoesNotReadUserOrHubData() {
        properties.setEnabled(false);

        assertThatThrownBy(() -> service.getHub("me", 3))
                .isInstanceOfSatisfying(AppException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MY_HUB_FEATURE_DISABLED));

        verifyNoInteractions(userRepository, queryRepository, s3UploadService);
    }

    @Test
    void imageVisibilityGateAlsoKeepsFeatureDisabled() {
        properties.setImageVisibilityReady(false);

        assertThatThrownBy(() -> service.getHub("me", 3))
                .isInstanceOfSatisfying(AppException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.MY_HUB_FEATURE_DISABLED));

        verifyNoInteractions(userRepository, queryRepository, s3UploadService);
    }

    @Test
    void buildsNormalHubWithStableCountsSectionsAndDateOnlyPreviews() {
        when(userRepository.findById("me")).thenReturn(Optional.of(user("me", " Plate ", " 서울 성동구 ", false)));
        when(queryRepository.findCounts("me"))
                .thenReturn(new CountsRow(5, 7, 24, 36, 5, 2));
        ContentRow video = new ContentRow(
                "VIDEO", 128, "place-1", "성수동 파스타", "video-thumb.jpg", null,
                "플레이트 키친", "서울 성동구", 37.5, 127.0,
                "me", "Plate", "profile.jpg",
                LocalDate.of(2026, 7, 15), null
        );
        ContentRow image = new ContentRow(
                "IMAGE", 456, "place-2", "오늘의 디저트", null, "first.jpg,second.jpg",
                "카페 접시", "서울숲길", 37.6, 127.1,
                "friend", "친구", null,
                LocalDate.of(2026, 7, 14), LocalDate.of(2026, 7, 15)
        );
        when(queryRepository.findRecentContent("me", 1)).thenReturn(List.of(video));
        when(queryRepository.findRecentLikes("me", 1)).thenReturn(List.of(image));
        when(s3UploadService.toImageUrl("video-thumb.jpg")).thenReturn("https://cdn/video-thumb.jpg");
        when(s3UploadService.toFeedImageUrl("first.jpg")).thenReturn("https://cdn/first.jpg");

        MyHubResponse result = service.getHub("me", 1);

        assertThat(result.profile().displayName()).isEqualTo("Plate");
        assertThat(result.profile().activeRegion()).isEqualTo("서울 성동구");
        assertThat(result.counts().contentCount()).isEqualTo(12);
        assertThat(result.availableSections()).containsExactly(Section.RECENT_CONTENT, Section.LIKED_CONTENT);
        assertThat(result.primaryAction()).isNull();
        assertThat(result.generatedAt()).isNotNull();

        MyHubResponse.ContentPreview content = result.recentContent().get(0);
        assertThat(content.contentType()).isEqualTo(ContentType.VIDEO);
        assertThat(content.contentId()).isEqualTo("video:128");
        assertThat(content.videoStoreId()).isEqualTo(128);
        assertThat(content.imageFeedId()).isNull();
        assertThat(content.thumbnailUrl()).isEqualTo("https://cdn/video-thumb.jpg");
        assertThat(content.createdAt()).isNull();
        assertThat(content.createdOn()).isEqualTo(LocalDate.of(2026, 7, 15));
        assertThat(content.createdTimePrecision()).isEqualTo(TimePrecision.DATE);
        assertThat(content.likedTimePrecision()).isNull();

        MyHubResponse.ContentPreview liked = result.recentLikes().get(0);
        assertThat(liked.contentType()).isEqualTo(ContentType.IMAGE);
        assertThat(liked.contentId()).isEqualTo("image:456");
        assertThat(liked.thumbnailUrl()).isEqualTo("https://cdn/first.jpg");
        assertThat(liked.likedAt()).isNull();
        assertThat(liked.likedOn()).isEqualTo(LocalDate.of(2026, 7, 15));
        assertThat(liked.likedTimePrecision()).isEqualTo(TimePrecision.DATE);
    }

    @Test
    void previewLimitZeroKeepsSectionsButSkipsPreviewQueries() {
        when(userRepository.findById("me")).thenReturn(Optional.of(user("me", null, "Seoul", false)));
        when(queryRepository.findCounts("me"))
                .thenReturn(new CountsRow(1, 0, 2, 0, 1, 0));

        MyHubResponse result = service.getHub("me", 0);

        assertThat(result.availableSections()).containsExactly(Section.RECENT_CONTENT, Section.LIKED_CONTENT);
        assertThat(result.recentContent()).isEmpty();
        assertThat(result.recentLikes()).isEmpty();
        verify(queryRepository, never()).findRecentContent("me", 0);
        verify(queryRepository, never()).findRecentLikes("me", 0);
    }

    @Test
    void coldStartUsesExploreContentBeforeOtherActions() {
        when(userRepository.findById("new-user"))
                .thenReturn(Optional.of(user("new-user", " ", null, false)));
        when(queryRepository.findCounts("new-user"))
                .thenReturn(new CountsRow(0, 0, 0, 0, 0, 0));

        MyHubResponse result = service.getHub("new-user", 3);

        assertThat(result.profile().displayName()).isEqualTo("new-user");
        assertThat(result.availableSections()).isEmpty();
        assertThat(result.primaryAction()).isEqualTo(PrimaryAction.EXPLORE_CONTENT);
        verify(queryRepository, never()).findRecentContent("new-user", 3);
        verify(queryRepository, never()).findRecentLikes("new-user", 3);
    }

    @Test
    void actionPriorityUsesRegionThenContentThenFriends() {
        when(userRepository.findById("me")).thenReturn(Optional.of(user("me", "Me", null, false)));
        when(queryRepository.findCounts("me"))
                .thenReturn(new CountsRow(1, 0, 1, 0, 0, 0));
        when(queryRepository.findRecentContent("me", 1)).thenReturn(List.of());
        when(queryRepository.findRecentLikes("me", 1)).thenReturn(List.of());
        assertThat(service.getHub("me", 1).primaryAction()).isEqualTo(PrimaryAction.SET_ACTIVE_REGION);

        when(userRepository.findById("me")).thenReturn(Optional.of(user("me", "Me", "Seoul", false)));
        when(queryRepository.findCounts("me"))
                .thenReturn(new CountsRow(0, 0, 1, 0, 2, 0));
        assertThat(service.getHub("me", 0).primaryAction()).isEqualTo(PrimaryAction.CREATE_CONTENT);

        when(queryRepository.findCounts("me"))
                .thenReturn(new CountsRow(1, 0, 0, 0, 0, 0));
        assertThat(service.getHub("me", 0).primaryAction()).isEqualTo(PrimaryAction.FIND_FRIENDS);
    }

    @Test
    void nullablePrivacyFailsInsteadOfPretendingAccountIsPublic() {
        when(userRepository.findById("legacy")).thenReturn(Optional.of(user("legacy", null, null, null)));

        assertThatThrownBy(() -> service.getHub("legacy", 0))
                .isInstanceOfSatisfying(AppException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COMMON_INTERNAL_ERROR));

        verifyNoInteractions(queryRepository);
    }

    private User user(String username, String nickname, String activeRegion, Boolean isPrivate) {
        return User.builder()
                .username(username)
                .nickname(nickname)
                .activeRegion(activeRegion)
                .isPrivate(isPrivate)
                .build();
    }
}
