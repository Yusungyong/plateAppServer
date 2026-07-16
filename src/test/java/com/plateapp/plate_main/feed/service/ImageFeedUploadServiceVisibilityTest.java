package com.plateapp.plate_main.feed.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.plateapp.plate_main.comment.repository.FeedCommentRepository;
import com.plateapp.plate_main.comment.repository.FeedReplyRepository;
import com.plateapp.plate_main.common.image.ImageProcessingService;
import com.plateapp.plate_main.common.s3.S3UploadService;
import com.plateapp.plate_main.feed.entity.Fp400ImageFeed;
import com.plateapp.plate_main.feed.dto.ImageFeedUploadResponse;
import com.plateapp.plate_main.feed.repository.ImageFeedRepository;
import com.plateapp.plate_main.friend.repository.Fp200VisitRepository;
import com.plateapp.plate_main.like.repository.ImageFeedLikeRepository;
import com.plateapp.plate_main.menu.repository.Fp320MenuRepository;
import com.plateapp.plate_main.restaurant.repository.RestaurantRepository;
import com.plateapp.plate_main.video.service.PlaceService;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class ImageFeedUploadServiceVisibilityTest {

    @Mock private ImageFeedRepository imageFeedRepository;
    @Mock private FeedCommentRepository feedCommentRepository;
    @Mock private FeedReplyRepository feedReplyRepository;
    @Mock private ImageFeedLikeRepository imageFeedLikeRepository;
    @Mock private Fp200VisitRepository fp200VisitRepository;
    @Mock private Fp320MenuRepository fp320MenuRepository;
    @Mock private S3UploadService s3UploadService;
    @Mock private ImageProcessingService imageProcessingService;
    @Mock private PlaceService placeService;
    @Mock private RestaurantRepository restaurantRepository;

    @InjectMocks
    private ImageFeedUploadService service;

    private Fp400ImageFeed feed;

    @BeforeEach
    void setUp() {
        feed = new Fp400ImageFeed();
        feed.setUsername("owner");
        feed.setContent("before");
        feed.setLocation("before-address");
        feed.setUseYn("Y");
    }

    @Test
    void updatePersistsNormalizedVisibility() {
        stubExistingFeed();
        service.updateFeed(
                1, "after", "after-address", null, null, null, null, null,
                "n", null, "owner"
        );

        assertThat(feed.getOpenYn()).isEqualTo("N");
        verify(imageFeedRepository).save(feed);
    }

    @Test
    void omittedCreateVisibilityStaysUnclassifiedButKeepsLegacyResponse() throws Exception {
        when(imageProcessingService.resizeMaxToTempFile(
                any(Path.class), anyInt(), anyInt(), eq("jpg")
        )).thenAnswer(invocation -> invocation.getArgument(0));
        when(imageProcessingService.resizeCropCenterToTempFile(
                any(Path.class), anyInt(), anyInt(), eq("jpg")
        )).thenAnswer(invocation -> invocation.getArgument(0));
        when(imageFeedRepository.save(any(Fp400ImageFeed.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        MockMultipartFile image = new MockMultipartFile(
                "files", "image.jpg", "image/jpeg", new byte[]{1, 2, 3}
        );

        ImageFeedUploadResponse response = service.createFeed(
                List.of(image), "content", "address", null, null, null,
                null, null, null, null, null, "owner"
        );

        assertThat(response.openYn).isEqualTo("Y");
        verify(imageFeedRepository).save(org.mockito.ArgumentMatchers.argThat(saved ->
                saved.getOpenYn() == null
        ));
    }

    @Test
    void omittedVisibilityPreservesLegacyNull() {
        stubExistingFeed();
        service.updateFeed(
                1, "after", "after-address", null, null, null, null, null,
                null, null, "owner"
        );

        assertThat(feed.getOpenYn()).isNull();
        verify(imageFeedRepository).save(feed);
    }

    @Test
    void invalidVisibilityIsRejected() {
        stubExistingFeed();
        assertThatThrownBy(() -> service.updateFeed(
                1, "after", "after-address", null, null, null, null, null,
                "public", null, "owner"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("value must be Y or N");

        verify(imageFeedRepository, never()).save(any());
    }

    private void stubExistingFeed() {
        when(imageFeedRepository.findById(1)).thenReturn(Optional.of(feed));
    }
}
