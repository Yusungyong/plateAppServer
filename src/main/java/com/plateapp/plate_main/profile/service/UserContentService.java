package com.plateapp.plate_main.profile.service;

import com.plateapp.plate_main.profile.dto.UserContentDtos.UserImageItem;
import com.plateapp.plate_main.profile.dto.UserContentDtos.UserVideoItem;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserContentService {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    public List<UserVideoItem> findUserVideos(String username, int limit, int offset) {
        String sql = """
            SELECT
              s.store_id,
              COALESCE(s.title, s.store_name) AS title,
              s.thumbnail,
              s.file_name,
              s.video_duration,
              s.place_id,
              s.store_name,
              s.address,
              s.created_at,
              s.updated_at
            FROM fp_300 s
            WHERE s.username = :username
              AND s.use_yn = 'Y'
              AND s.open_yn = 'Y'
              AND s.deleted_at IS NULL
            ORDER BY s.created_at DESC NULLS LAST, s.store_id DESC
            LIMIT :limit OFFSET :offset
            """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("username", username)
                .addValue("limit", limit)
                .addValue("offset", offset);

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> new UserVideoItem(
                rs.getInt("store_id"),
                rs.getString("title"),
                rs.getString("thumbnail"),
                rs.getString("file_name"),
                (Integer) rs.getObject("video_duration"),
                rs.getString("place_id"),
                rs.getString("store_name"),
                rs.getString("address"),
                toLocalDateTime(rs.getTimestamp("created_at")),
                toLocalDateTime(rs.getTimestamp("updated_at"))
        ));
    }

    @Transactional(readOnly = true)
    public List<UserImageItem> findUserImages(String username, int limit, int offset) {
        String sql = """
            SELECT
              f.feed_no,
              COALESCE(f.feed_title, f.store_name) AS title,
              f.thumbnail,
              f.images,
              f.place_id,
              f.store_name,
              f.created_at,
              f.updated_at
            FROM fp_400 f
            WHERE f.username = :username
              AND f.use_yn = 'Y'
            ORDER BY f.created_at DESC NULLS LAST, f.feed_no DESC
            LIMIT :limit OFFSET :offset
            """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("username", username)
                .addValue("limit", limit)
                .addValue("offset", offset);

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> {
            String thumbnail = rs.getString("thumbnail");
            if (thumbnail == null || thumbnail.isBlank()) {
                String images = rs.getString("images");
                if (images != null && !images.isBlank()) {
                    thumbnail = images.split(",")[0].trim();
                }
            }

            return new UserImageItem(
                    rs.getInt("feed_no"),
                    rs.getString("title"),
                    thumbnail,
                    rs.getString("place_id"),
                    rs.getString("store_name"),
                    toLocalDateTime(rs.getTimestamp("created_at")),
                    toLocalDateTime(rs.getTimestamp("updated_at"))
            );
        });
    }

    private static java.time.LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }
}
