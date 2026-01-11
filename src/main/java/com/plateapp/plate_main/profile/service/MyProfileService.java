package com.plateapp.plate_main.profile.service;

import com.plateapp.plate_main.auth.domain.User;
import com.plateapp.plate_main.auth.repository.UserRepository;
import com.plateapp.plate_main.profile.dto.MyProfileRequest;
import com.plateapp.plate_main.profile.dto.MyProfileResponse;
import com.plateapp.plate_main.profile.dto.MyProfileResponse.Settings;
import com.plateapp.plate_main.profile.dto.MyProfileResponse.Stats;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MyProfileService {

    private final UserRepository userRepository;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    public MyProfileResponse getProfile(MyProfileRequest request) {
        String username = request.username();
        boolean includeStats = Boolean.TRUE.equals(request.includeStats());

        User user = userRepository.findById(username)
                .orElseThrow(() -> new IllegalArgumentException("ì¡´ìž¬?˜ì? ?ŠëŠ” ?¬ìš©?? " + username));

        Stats stats = includeStats ? fetchStats(username) : new Stats(0, 0, 0, 0, 0);
        Settings settings = defaultSettings();

        Integer userId = userRepository.findUserIdByUsername(username);

        return MyProfileResponse.of(
                userId,
                user.getUsername(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getCreatedAt(),
                stats,
                settings
        );
    }

    private Settings defaultSettings() {
        // 기본 설정 (푸시 알림 허용, 마케팅 알림 미허용, 기본 언어 ko)
        return new Settings(true, false, "ko");
    }

    private Stats fetchStats(String username) {
        String sql = """
            SELECT
              (SELECT COUNT(*) FROM fp_50  WHERE username = :username AND use_yn = 'Y' AND deleted_at IS NULL) AS video_like_count,
              (SELECT COUNT(*) FROM fp_60  WHERE username = :username AND use_yn = 'Y' AND deleted_at IS NULL) AS image_like_count,
              (SELECT COUNT(*) FROM fp_440 WHERE username = :username AND use_yn = 'Y' AND deleted_at IS NULL) AS video_comment_count,
              (SELECT COUNT(*) FROM fp_460 WHERE username = :username AND use_yn = 'Y' AND deleted_at IS NULL) AS image_comment_count,
              (SELECT COUNT(*) FROM fp_300 WHERE username = :username AND use_yn = 'Y' AND deleted_at IS NULL) AS video_post_count,
              (SELECT COUNT(*) FROM fp_400 WHERE username = :username AND use_yn = 'Y') AS image_post_count
            """;

        MapSqlParameterSource params = new MapSqlParameterSource("username", username);
        return jdbcTemplate.queryForObject(sql, params, (rs, rowNum) -> {
            long likeCount = rs.getLong("video_like_count") + rs.getLong("image_like_count");
            long commentCount = rs.getLong("video_comment_count") + rs.getLong("image_comment_count");
            long videoPostCount = rs.getLong("video_post_count");
            long imagePostCount = rs.getLong("image_post_count");
            long totalPostCount = videoPostCount + imagePostCount;
            return new Stats(likeCount, commentCount, videoPostCount, imagePostCount, totalPostCount);
        });
    }
}
