package com.plateapp.plate_main.profile.service;

import com.plateapp.plate_main.auth.domain.LoginHistory;
import com.plateapp.plate_main.auth.domain.SocialAccount;
import com.plateapp.plate_main.auth.domain.User;
import com.plateapp.plate_main.auth.repository.LoginHistoryRepository;
import com.plateapp.plate_main.auth.repository.SocialAccountRepository;
import com.plateapp.plate_main.auth.repository.UserRepository;
import com.plateapp.plate_main.feed.repository.ImageFeedRepository;
import com.plateapp.plate_main.friend.repository.Fp150FriendRepository;
import com.plateapp.plate_main.profile.dto.ProfileDetailResponse;
import com.plateapp.plate_main.video.repository.Fp300StoreRepository;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileDetailService {

    private static final String STATUS_ACCEPTED = "accepted";

    private final UserRepository userRepository;
    private final Fp150FriendRepository friendRepository;
    private final ImageFeedRepository imageFeedRepository;
    private final Fp300StoreRepository storeRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    public ProfileDetailResponse getPublicProfileDetail(String username) {
        User user = loadUser(username);
        ProfileDetailResponse.Stats stats = loadStats(username);
        ProfileDetailResponse.Friends friends = ProfileDetailResponse.Friends.builder()
                .count(friendRepository.countByUsernameAndStatus(username, STATUS_ACCEPTED))
                .build();

        return ProfileDetailResponse.builder()
                .username(user.getUsername())
                .nickName(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .activeRegion(user.getActiveRegion())
                .stats(stats)
                .friends(friends)
                .build();
    }

    @Transactional(readOnly = true)
    public ProfileDetailResponse getMyProfileDetail(String username) {
        User user = loadUser(username);
        ProfileDetailResponse.Stats stats = loadStats(username);
        ProfileDetailResponse.Friends friends = ProfileDetailResponse.Friends.builder()
                .count(friendRepository.countByUsernameAndStatus(username, STATUS_ACCEPTED))
                .build();

        ProfileDetailResponse.Account account = ProfileDetailResponse.Account.builder()
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();

        ProfileDetailResponse.Social social = loadSocial(user);
        ProfileDetailResponse.Login login = loadLogin(username);
        ProfileDetailResponse.Preferences preferences = loadPreferences(username);
        ProfileDetailResponse.Safety safety = loadSafety(username);

        return ProfileDetailResponse.builder()
                .username(user.getUsername())
                .nickName(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .activeRegion(user.getActiveRegion())
                .stats(stats)
                .friends(friends)
                .account(account)
                .social(social)
                .login(login)
                .preferences(preferences)
                .safety(safety)
                .build();
    }

    private User loadUser(String username) {
        return userRepository.findById(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }

    @Transactional(readOnly = true)
    public ProfileDetailResponse.Stats loadStats(String username) {
        long videoCount = storeRepository.countByUsernameAndUseYn(username, "Y");
        long imageCount = imageFeedRepository.countByUsernameAndUseYn(username, "Y");
        long likeCount = getLikeCount(username);
        LocalDateTime recentActivityAt = getRecentActivityAt(username);

        return ProfileDetailResponse.Stats.builder()
                .videoCount(videoCount)
                .imageCount(imageCount)
                .likeCount(likeCount)
                .recentActivityAt(recentActivityAt)
                .build();
    }

    private long getLikeCount(String username) {
        String sql = """
            SELECT
              (SELECT COUNT(*) FROM fp_50  WHERE username = :username AND use_yn = 'Y' AND deleted_at IS NULL) AS video_like_count,
              (SELECT COUNT(*) FROM fp_60  WHERE username = :username AND use_yn = 'Y' AND deleted_at IS NULL) AS image_like_count
            """;

        MapSqlParameterSource params = new MapSqlParameterSource("username", username);
        return jdbcTemplate.queryForObject(sql, params, (rs, rowNum) ->
                rs.getLong("video_like_count") + rs.getLong("image_like_count"));
    }

    private LocalDateTime getRecentActivityAt(String username) {
        String sql = """
            SELECT
              (SELECT MAX(updated_at)::timestamp FROM fp_300 WHERE username = :username AND use_yn = 'Y' AND deleted_at IS NULL) AS video_updated_at,
              (SELECT MAX(updated_at) FROM fp_400 WHERE username = :username AND use_yn = 'Y') AS image_updated_at,
              (SELECT MAX(created_at) FROM fp_440 WHERE username = :username AND use_yn = 'Y' AND deleted_at IS NULL) AS video_comment_at,
              (SELECT MAX(created_at) FROM fp_460 WHERE username = :username AND use_yn = 'Y' AND deleted_at IS NULL) AS image_comment_at
            """;

        MapSqlParameterSource params = new MapSqlParameterSource("username", username);
        return jdbcTemplate.queryForObject(sql, params, (rs, rowNum) -> {
            LocalDateTime videoAt = toLocalDateTime(rs.getTimestamp("video_updated_at"));
            LocalDateTime imageAt = toLocalDateTime(rs.getTimestamp("image_updated_at"));
            LocalDateTime videoCommentAt = toLocalDateTime(rs.getTimestamp("video_comment_at"));
            LocalDateTime imageCommentAt = toLocalDateTime(rs.getTimestamp("image_comment_at"));

            return maxOf(videoAt, imageAt, videoCommentAt, imageCommentAt);
        });
    }

    private ProfileDetailResponse.Social loadSocial(User user) {
        Integer userId = user.getUserId();
        if (userId == null) {
            return null;
        }

        SocialAccount account = socialAccountRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .orElse(null);
        if (account == null) {
            return null;
        }

        return ProfileDetailResponse.Social.builder()
                .provider(account.getProvider())
                .providerUserId(account.getProviderUserId())
                .displayName(account.getDisplayName())
                .build();
    }

    private ProfileDetailResponse.Login loadLogin(String username) {
        LoginHistory history = loginHistoryRepository.findTop1ByUsernameOrderByLoginDatetimeDesc(username);
        if (history == null) {
            return null;
        }

        return ProfileDetailResponse.Login.builder()
                .lastLoginAt(toLocalDateTime(history.getLoginDatetime()))
                .lastLoginIp(history.getIpAddress())
                .lastLoginStatus(history.getLoginStatus())
                .lastFailReason(history.getFailReason())
                .build();
    }

    private ProfileDetailResponse.Preferences loadPreferences(String username) {
        String sql = """
            SELECT filter_type, image_yn, time_filter, region_filter, post_sorted
            FROM fp_410
            WHERE username = :username
            ORDER BY updated_at DESC NULLS LAST
            LIMIT 1
            """;

        MapSqlParameterSource params = new MapSqlParameterSource("username", username);
        return jdbcTemplate.query(sql, params, rs -> {
            if (!rs.next()) {
                return null;
            }
            ProfileDetailResponse.Filters filters = ProfileDetailResponse.Filters.builder()
                    .filterType(rs.getString("filter_type"))
                    .imageYn("Y".equalsIgnoreCase(rs.getString("image_yn")))
                    .timeFilter(rs.getString("time_filter"))
                    .regionFilter(rs.getString("region_filter"))
                    .postSorted(rs.getString("post_sorted"))
                    .build();

            return ProfileDetailResponse.Preferences.builder()
                    .notifications(null)
                    .filters(filters)
                    .build();
        });
    }

    private ProfileDetailResponse.Safety loadSafety(String username) {
        String sql = """
            SELECT
              (SELECT COUNT(*) FROM fp_160 WHERE blocker_username = :username) AS blocked_count,
              (SELECT COUNT(*) FROM fp_40 WHERE reporter_username = :username) AS report_count
            """;

        MapSqlParameterSource params = new MapSqlParameterSource("username", username);
        return jdbcTemplate.queryForObject(sql, params, (rs, rowNum) ->
                ProfileDetailResponse.Safety.builder()
                        .blockedCount(rs.getLong("blocked_count"))
                        .reportCount(rs.getLong("report_count"))
                        .build());
    }

    private LocalDateTime toLocalDateTime(OffsetDateTime value) {
        return value == null ? null : value.toLocalDateTime();
    }

    private LocalDateTime toLocalDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }

    private LocalDateTime maxOf(LocalDateTime... values) {
        LocalDateTime max = null;
        for (LocalDateTime value : values) {
            if (value == null) {
                continue;
            }
            max = (max == null || value.isAfter(max)) ? value : max;
        }
        return max;
    }
}
