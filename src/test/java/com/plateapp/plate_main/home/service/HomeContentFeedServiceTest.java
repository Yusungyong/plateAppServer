package com.plateapp.plate_main.home.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.plateapp.plate_main.block.repository.BlockRepository;
import com.plateapp.plate_main.comment.repository.FeedCommentRepository;
import com.plateapp.plate_main.comment.repository.ReplyRepository;
import com.plateapp.plate_main.common.s3.S3UploadService;
import com.plateapp.plate_main.feed.entity.Fp400Feed;
import com.plateapp.plate_main.feed.repository.Fp400FeedRepository;
import com.plateapp.plate_main.home.dto.HomeContentFeedResponse;
import com.plateapp.plate_main.like.repository.FeedLikeRepository;
import com.plateapp.plate_main.like.service.LikeService;
import com.plateapp.plate_main.report.repository.ReportRepository;
import com.plateapp.plate_main.user.repository.MemberRepository;
import com.plateapp.plate_main.video.entity.Fp300Store;
import com.plateapp.plate_main.video.repository.Fp300StoreRepository;
import com.plateapp.plate_main.video.repository.Fp440CommentRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HomeContentFeedServiceTest {

    @Mock
    private Fp300StoreRepository videoRepository;
    @Mock
    private Fp400FeedRepository imageRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private LikeService likeService;
    @Mock
    private FeedLikeRepository feedLikeRepository;
    @Mock
    private Fp440CommentRepository videoCommentRepository;
    @Mock
    private FeedCommentRepository feedCommentRepository;
    @Mock
    private ReplyRepository replyRepository;
    @Mock
    private BlockRepository blockRepository;
    @Mock
    private ReportRepository reportRepository;
    @Mock
    private S3UploadService s3UploadService;
    @Mock
    private HomeImpressionService homeImpressionService;

    private HomeContentFeedService service;

    @BeforeEach
    void setUp() {
        service = new HomeContentFeedService(
                videoRepository,
                imageRepository,
                memberRepository,
                likeService,
                feedLikeRepository,
                videoCommentRepository,
                feedCommentRepository,
                replyRepository,
                blockRepository,
                reportRepository,
                s3UploadService,
                homeImpressionService
        );

        when(blockRepository.findBlockedUsernames(anyString())).thenReturn(List.of());
        when(reportRepository.findReportedUsernames(anyString())).thenReturn(List.of());
        org.mockito.Mockito.lenient().when(memberRepository.findByUsernameIn(org.mockito.ArgumentMatchers.<String>anyList())).thenReturn(List.of());
        when(likeService.getLikeCountMap(anyList())).thenReturn(Map.of());
        when(likeService.getMyLikedStoreIdSet(anyString(), anyList())).thenReturn(Set.of());
        when(feedLikeRepository.findMyActiveLikedFeedIds(anyString(), anyList())).thenReturn(List.of());
        when(feedLikeRepository.countActiveByFeedIds(anyList())).thenReturn(List.of());
        when(videoCommentRepository.countActiveByStoreIds(anyList())).thenReturn(List.of());
        when(feedCommentRepository.countActiveByFeedIds(anyList())).thenReturn(List.of());
        when(replyRepository.countActiveByFeedIds(anyList())).thenReturn(List.of());
        when(homeImpressionService.loadRecentExclusion(anyString(), org.mockito.ArgumentMatchers.anyBoolean(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(HomeImpressionExclusion.empty());

        when(s3UploadService.toImageUrl(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        when(s3UploadService.toVideoUrl(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        when(s3UploadService.toFeedImageUrl(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void searchContentFeedKeepsHomeCardShape() {
        Fp300Store video = video(101, "Gwanghwamun Pasta", "Seoul Jongno-gu", "chef1", "thumb-101", "video-101");
        Fp400Feed image = image(201, "Gwanghwamun Dessert", "Strawberry Parfait", "Seoul Jongno-gu", "chef2", "20260517/A.jpg");

        when(videoRepository.findLatestForHome(org.mockito.ArgumentMatchers.any(Pageable.class))).thenReturn(List.of(video));
        when(imageRepository.findLatestForHomeByGroup(org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(List.of(image));

        HomeContentFeedResponse response = service.searchContentFeed(
                "Gwanghwamun",
                null,
                10,
                "home-content-search",
                "viewer",
                false,
                null,
                null,
                null,
                null
        );

        assertNotNull(response);
        assertNotNull(response.generatedAt());
        assertTrue(response.trackingToken().startsWith("home-content-search-"));
        assertEquals(2, response.items().size());
        assertNull(response.nextCursor());

        response.items().forEach(item -> {
            assertNotNull(item.feedKey());
            assertNotNull(item.contentType());
            assertNotNull(item.author());
            assertNotNull(item.stats());
            assertNotNull(item.createdAt());
        });
    }

    @Test
    void searchContentFeedSupportsCursorPaging() {
        Fp300Store latest = video(101, "Pasta Place", "Seoul Jongno-gu", "chef1", "thumb-101", "video-101");
        latest.setCreatedAt(LocalDate.now());
        Fp300Store older = video(102, "Pasta Lunch", "Seoul Mapo-gu", "chef2", "thumb-102", "video-102");
        older.setCreatedAt(LocalDate.now().minusDays(1));

        when(videoRepository.findLatestForHome(org.mockito.ArgumentMatchers.any(Pageable.class))).thenReturn(List.of(latest, older));
        when(imageRepository.findLatestForHomeByGroup(org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(List.of());

        HomeContentFeedResponse first = service.searchContentFeed(
                "Pasta",
                null,
                1,
                "home-content-search",
                "viewer",
                false,
                null,
                null,
                null,
                null
        );

        assertEquals(1, first.items().size());
        assertNotNull(first.nextCursor());

        HomeContentFeedResponse second = service.searchContentFeed(
                "Pasta",
                first.nextCursor(),
                1,
                "home-content-search",
                "viewer",
                false,
                null,
                null,
                null,
                null
        );

        assertEquals(1, second.items().size());
        assertNull(second.nextCursor());
        assertFalse(first.items().get(0).feedKey().equals(second.items().get(0).feedKey()));
    }

    private Fp300Store video(
            Integer storeId,
            String title,
            String address,
            String username,
            String thumbnail,
            String fileName
    ) {
        Fp300Store store = new Fp300Store();
        store.setStoreId(storeId);
        store.setTitle(title);
        store.setStoreName(title);
        store.setAddress(address);
        store.setUsername(username);
        store.setThumbnail(thumbnail);
        store.setFileName(fileName);
        store.setCreatedAt(LocalDate.now());
        store.setUpdatedAt(LocalDate.now());
        store.setPlaceId("place-" + storeId);
        store.setVideoDuration(15);
        return store;
    }

    private Fp400Feed image(
            Integer feedId,
            String title,
            String content,
            String location,
            String username,
            String imagePath
    ) {
        Fp400Feed feed = new Fp400Feed();
        ReflectionTestUtils.setField(feed, "feedNo", feedId);
        ReflectionTestUtils.setField(feed, "feedTitle", title);
        ReflectionTestUtils.setField(feed, "content", content);
        ReflectionTestUtils.setField(feed, "location", location);
        ReflectionTestUtils.setField(feed, "username", username);
        ReflectionTestUtils.setField(feed, "images", imagePath);
        ReflectionTestUtils.setField(feed, "placeId", "place-" + feedId);
        ReflectionTestUtils.setField(feed, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(feed, "updatedAt", LocalDateTime.now());
        ReflectionTestUtils.setField(feed, "storeName", title);
        return feed;
    }
}
