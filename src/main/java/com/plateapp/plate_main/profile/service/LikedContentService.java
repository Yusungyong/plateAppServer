package com.plateapp.plate_main.profile.service;

import com.plateapp.plate_main.profile.dto.LikedContentDtos.LikedImageItem;
import com.plateapp.plate_main.profile.dto.LikedContentDtos.LikedVideoItem;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LikedContentService {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    public List<LikedVideoItem> findLikedVideos(String username, int limit, int offset) {
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
              l.created_at AS liked_at
            FROM fp_50 l
            JOIN fp_300 s ON s.store_id = l.store_id
            WHERE l.username = :username
              AND l.use_yn = 'Y'
              AND l.deleted_at IS NULL
              AND s.use_yn = 'Y'
              AND s.open_yn = 'Y'
              AND s.deleted_at IS NULL
            ORDER BY l.created_at DESC NULLS LAST, s.updated_at DESC NULLS LAST, s.store_id DESC
            LIMIT :limit OFFSET :offset
            """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("username", username)
                .addValue("limit", limit)
                .addValue("offset", offset);

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> new LikedVideoItem(
                rs.getInt("store_id"),
                rs.getString("title"),
                rs.getString("thumbnail"),
                rs.getString("file_name"),
                (Integer) rs.getObject("video_duration"),
                rs.getString("place_id"),
                rs.getString("store_name"),
                rs.getString("address"),
                rs.getTimestamp("liked_at") != null ? rs.getTimestamp("liked_at").toLocalDateTime() : null
        ));
    }

    @Transactional(readOnly = true)
    public List<LikedImageItem> findLikedImages(String username, int limit, int offset) {
        String sql = """
            SELECT
              f.feed_no AS feed_id,
              COALESCE(f.feed_title, f.store_name) AS title,
              f.thumbnail,
              f.place_id,
              f.store_name,
              l.created_at AS liked_at
            FROM fp_60 l
            JOIN fp_400 f ON f.feed_no = l.feed_id
            WHERE l.username = :username
              AND l.use_yn = 'Y'
              AND l.deleted_at IS NULL
              AND f.use_yn = 'Y'
            ORDER BY l.created_at DESC NULLS LAST, f.updated_at DESC NULLS LAST, f.feed_no DESC
            LIMIT :limit OFFSET :offset
            """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("username", username)
                .addValue("limit", limit)
                .addValue("offset", offset);

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> new LikedImageItem(
                rs.getInt("feed_id"),
                rs.getString("title"),
                rs.getString("thumbnail"),
                rs.getString("place_id"),
                rs.getString("store_name"),
                rs.getTimestamp("liked_at") != null ? rs.getTimestamp("liked_at").toLocalDateTime() : null
        ));
    }
}
