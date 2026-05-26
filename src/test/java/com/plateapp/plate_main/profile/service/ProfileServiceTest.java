package com.plateapp.plate_main.profile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.plateapp.plate_main.auth.repository.RefreshTokenRepository;
import com.plateapp.plate_main.auth.repository.SocialAccountRepository;
import com.plateapp.plate_main.auth.repository.UserRepository;
import com.plateapp.plate_main.auth.service.ProfileHistoryService;
import com.plateapp.plate_main.auth.service.SocialAuthService;
import com.plateapp.plate_main.common.image.ImageProcessingService;
import com.plateapp.plate_main.common.s3.S3UploadService;
import com.plateapp.plate_main.feed.repository.ImageFeedRepository;
import com.plateapp.plate_main.friend.repository.Fp150FriendRepository;
import com.plateapp.plate_main.notification.service.UserPushTokenService;
import com.plateapp.plate_main.profile.dto.UserStatsDTO;
import com.plateapp.plate_main.video.repository.Fp300StoreRepository;
import com.plateapp.plate_main.video.repository.Fp305WatchHistoryRepository;
import com.plateapp.plate_main.video.repository.Fp440CommentRepository;
import java.sql.ResultSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private Fp150FriendRepository friendRepository;
    @Mock private ImageFeedRepository imageFeedRepository;
    @Mock private Fp300StoreRepository storeRepository;
    @Mock private Fp305WatchHistoryRepository watchHistoryRepository;
    @Mock private Fp440CommentRepository commentRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private SocialAccountRepository socialAccountRepository;
    @Mock private S3UploadService s3UploadService;
    @Mock private ImageProcessingService imageProcessingService;
    @Mock private BCryptPasswordEncoder passwordEncoder;
    @Mock private ProfileHistoryService profileHistoryService;
    @Mock private SocialAuthService socialAuthService;
    @Mock private UserPushTokenService userPushTokenService;
    @Mock private NamedParameterJdbcTemplate jdbcTemplate;

    private ProfileService profileService;

    @BeforeEach
    void setUp() {
        profileService = new ProfileService(
                userRepository,
                friendRepository,
                imageFeedRepository,
                storeRepository,
                watchHistoryRepository,
                commentRepository,
                refreshTokenRepository,
                socialAccountRepository,
                s3UploadService,
                imageProcessingService,
                passwordEncoder,
                profileHistoryService,
                socialAuthService,
                userPushTokenService,
                jdbcTemplate
        );
    }

    @Test
    void getUserStatsUsesActiveLikeCountInsteadOfZero() {
        when(userRepository.findUserIdByUsername("tester")).thenReturn(1);
        when(friendRepository.countByUsernameAndStatus("tester", "accepted")).thenReturn(2L);
        when(imageFeedRepository.countByUsernameAndUseYn("tester", "Y")).thenReturn(3L);
        when(storeRepository.countByUsernameAndUseYn("tester", "Y")).thenReturn(4L);
        when(jdbcTemplate.queryForObject(
                any(String.class),
                any(MapSqlParameterSource.class),
                any(RowMapper.class)
        )).thenAnswer(invocation -> {
            RowMapper<Long> mapper = invocation.getArgument(2);
            ResultSet rs = org.mockito.Mockito.mock(ResultSet.class);
            when(rs.getLong("video_like_count")).thenReturn(5L);
            when(rs.getLong("image_like_count")).thenReturn(6L);
            return mapper.mapRow(rs, 0);
        });

        UserStatsDTO stats = profileService.getUserStats("tester");

        assertThat(stats.getFriendsCount()).isEqualTo(2L);
        assertThat(stats.getPostsCount()).isEqualTo(7L);
        assertThat(stats.getLikesCount()).isEqualTo(11L);
        assertThat(stats.getVisitedStoresCount()).isEqualTo(4L);
    }
}
