package com.plateapp.plate_main.mypage.repository;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MyHubQueryRepository {

    private static final String ACCEPTED_STATUSES = "('accepted', 'cd_002')";
    private static final String PENDING_STATUSES = "('pending', 'cd_001')";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public MyHubQueryRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public CountsRow findCounts(String username) {
        CountsRow result = jdbcTemplate.queryForObject(
                COUNTS_SQL,
                Map.of("username", username),
                (rs, rowNum) -> new CountsRow(
                        rs.getLong("video_count"),
                        rs.getLong("image_count"),
                        rs.getLong("liked_content_count"),
                        rs.getLong("received_like_count"),
                        rs.getLong("friend_count"),
                        rs.getLong("pending_friend_request_count")
                )
        );
        if (result == null) {
            throw new IllegalStateException("My hub count query returned no row");
        }
        return result;
    }

    public List<ContentRow> findRecentContent(String username, int limit) {
        return jdbcTemplate.query(
                RECENT_CONTENT_SQL,
                Map.of("username", username, "limit", limit),
                CONTENT_ROW_MAPPER
        );
    }

    public List<ContentRow> findRecentLikes(String username, int limit) {
        return jdbcTemplate.query(
                RECENT_LIKES_SQL,
                Map.of("username", username, "limit", limit),
                CONTENT_ROW_MAPPER
        );
    }

    public record CountsRow(
            long videoCount,
            long imageCount,
            long likedContentCount,
            long receivedLikeCount,
            long friendCount,
            long pendingFriendRequestCount
    ) {
    }

    public record ContentRow(
            String contentType,
            Integer sourceId,
            String placeId,
            String title,
            String thumbnailSource,
            String imagesSource,
            String storeName,
            String address,
            Double latitude,
            Double longitude,
            String authorUsername,
            String authorDisplayName,
            String authorProfileImageUrl,
            LocalDate createdOn,
            LocalDate likedOn
    ) {
    }

    private static final RowMapper<ContentRow> CONTENT_ROW_MAPPER = (rs, rowNum) -> new ContentRow(
            rs.getString("content_type"),
            getInteger(rs, "source_id"),
            rs.getString("place_id"),
            rs.getString("title"),
            rs.getString("thumbnail_source"),
            rs.getString("images_source"),
            rs.getString("store_name"),
            rs.getString("address"),
            getDouble(rs, "latitude"),
            getDouble(rs, "longitude"),
            rs.getString("author_username"),
            rs.getString("author_display_name"),
            rs.getString("author_profile_image_url"),
            getLocalDate(rs, "created_on"),
            getLocalDate(rs, "liked_on")
    );

    private static Integer getInteger(ResultSet rs, String column) throws SQLException {
        Number value = (Number) rs.getObject(column);
        return value == null ? null : value.intValue();
    }

    private static Double getDouble(ResultSet rs, String column) throws SQLException {
        Number value = (Number) rs.getObject(column);
        return value == null ? null : value.doubleValue();
    }

    private static LocalDate getLocalDate(ResultSet rs, String column) throws SQLException {
        Date value = rs.getDate(column);
        return value == null ? null : value.toLocalDate();
    }

    private static final String COUNTS_SQL = """
            WITH visible_liked AS (
                SELECT 'VIDEO' AS content_type, l.store_id AS content_id
                  FROM fp_50 l
                  JOIN fp_300 v ON v.store_id = l.store_id
                  JOIN fp_100 author ON author.username = v.username
                 WHERE l.username = :username
                   AND l.use_yn = 'Y'
                   AND l.deleted_at IS NULL
                   AND v.use_yn = 'Y'
                   AND v.deleted_at IS NULL
                   AND v.file_name IS NOT NULL
                   AND BTRIM(v.file_name) <> ''
                   AND (
                        v.username = :username
                        OR (
                            v.open_yn = 'Y'
                            AND (
                                COALESCE(author.is_private, TRUE) = FALSE
                                OR EXISTS (
                                    SELECT 1 FROM fp_150 fr
                                     WHERE LOWER(fr.status) IN """ + ACCEPTED_STATUSES + """
                                       AND ((fr.username = :username AND fr.friend_name = v.username)
                                         OR (fr.username = v.username AND fr.friend_name = :username))
                                )
                            )
                        )
                   )
                   AND NOT EXISTS (
                       SELECT 1 FROM fp_160 b
                        WHERE (b.blocker_username = :username AND b.blocked_username = v.username)
                           OR (b.blocker_username = v.username AND b.blocked_username = :username)
                   )
                   AND NOT EXISTS (
                       SELECT 1 FROM fp_40 r
                        WHERE r.reporter_username = :username
                          AND UPPER(BTRIM(r.target_flag)) = 'Y'
                          AND r.unflagged_at IS NULL
                          AND ((LOWER(r.target_type) = 'video' AND r.target_id = v.store_id)
                            OR (LOWER(r.target_type) = 'user' AND (
                                r.target_username = v.username
                                OR r.target_user_id = author.user_id
                                OR r.target_id = author.user_id
                            )))
                   )
                UNION ALL
                SELECT 'IMAGE' AS content_type, l.feed_id AS content_id
                  FROM fp_60 l
                  JOIN fp_400 f ON f.feed_no = l.feed_id
                  JOIN fp_100 author ON author.username = f.username
                 WHERE l.username = :username
                   AND l.use_yn = 'Y'
                   AND l.deleted_at IS NULL
                   AND f.use_yn = 'Y'
                   AND f.images IS NOT NULL
                   AND BTRIM(f.images) <> ''
                   AND (
                        f.username = :username
                        OR (
                            f.open_yn = 'Y'
                            AND (
                                COALESCE(author.is_private, TRUE) = FALSE
                                OR EXISTS (
                                    SELECT 1 FROM fp_150 fr
                                     WHERE LOWER(fr.status) IN """ + ACCEPTED_STATUSES + """
                                       AND ((fr.username = :username AND fr.friend_name = f.username)
                                         OR (fr.username = f.username AND fr.friend_name = :username))
                                )
                            )
                        )
                   )
                   AND NOT EXISTS (
                       SELECT 1 FROM fp_160 b
                        WHERE (b.blocker_username = :username AND b.blocked_username = f.username)
                           OR (b.blocker_username = f.username AND b.blocked_username = :username)
                   )
                   AND NOT EXISTS (
                       SELECT 1 FROM fp_40 r
                        WHERE r.reporter_username = :username
                          AND UPPER(BTRIM(r.target_flag)) = 'Y'
                          AND r.unflagged_at IS NULL
                          AND ((LOWER(r.target_type) = 'image' AND r.target_id = f.feed_no)
                            OR (LOWER(r.target_type) = 'user' AND (
                                r.target_username = f.username
                                OR r.target_user_id = author.user_id
                                OR r.target_id = author.user_id
                            )))
                   )
            ),
            valid_received AS (
                SELECT DISTINCT 'VIDEO' AS content_type, v.store_id AS content_id, l.username AS liker
                  FROM fp_300 v
                  JOIN fp_50 l ON l.store_id = v.store_id
                  JOIN fp_100 owner ON owner.username = v.username
                  JOIN fp_100 liker ON liker.username = l.username
                 WHERE v.username = :username
                   AND l.username <> :username
                   AND l.use_yn = 'Y'
                   AND l.deleted_at IS NULL
                   AND v.use_yn = 'Y'
                   AND v.open_yn = 'Y'
                   AND v.deleted_at IS NULL
                   AND v.file_name IS NOT NULL
                   AND BTRIM(v.file_name) <> ''
                   AND (
                        COALESCE(owner.is_private, TRUE) = FALSE
                        OR EXISTS (
                            SELECT 1 FROM fp_150 fr
                             WHERE LOWER(fr.status) IN """ + ACCEPTED_STATUSES + """
                               AND ((fr.username = l.username AND fr.friend_name = :username)
                                 OR (fr.username = :username AND fr.friend_name = l.username))
                        )
                   )
                   AND NOT EXISTS (
                       SELECT 1 FROM fp_160 b
                        WHERE (b.blocker_username = l.username AND b.blocked_username = :username)
                           OR (b.blocker_username = :username AND b.blocked_username = l.username)
                   )
                   AND NOT EXISTS (
                       SELECT 1 FROM fp_40 r
                        WHERE r.reporter_username = :username
                          AND UPPER(BTRIM(r.target_flag)) = 'Y'
                          AND r.unflagged_at IS NULL
                          AND LOWER(r.target_type) = 'user'
                          AND (r.target_username = l.username
                            OR r.target_user_id = liker.user_id
                            OR r.target_id = liker.user_id)
                   )
                   AND NOT EXISTS (
                       SELECT 1 FROM fp_40 r
                        WHERE r.reporter_username = l.username
                          AND UPPER(BTRIM(r.target_flag)) = 'Y'
                          AND r.unflagged_at IS NULL
                          AND ((LOWER(r.target_type) = 'video' AND r.target_id = v.store_id)
                            OR (LOWER(r.target_type) = 'user' AND (
                                r.target_username = :username
                                OR r.target_user_id = owner.user_id
                                OR r.target_id = owner.user_id
                            )))
                   )
                UNION ALL
                SELECT DISTINCT 'IMAGE' AS content_type, f.feed_no AS content_id, l.username AS liker
                  FROM fp_400 f
                  JOIN fp_60 l ON l.feed_id = f.feed_no
                  JOIN fp_100 owner ON owner.username = f.username
                  JOIN fp_100 liker ON liker.username = l.username
                 WHERE f.username = :username
                   AND l.username <> :username
                   AND l.use_yn = 'Y'
                   AND l.deleted_at IS NULL
                   AND f.use_yn = 'Y'
                   AND f.open_yn = 'Y'
                   AND f.images IS NOT NULL
                   AND BTRIM(f.images) <> ''
                   AND (
                        COALESCE(owner.is_private, TRUE) = FALSE
                        OR EXISTS (
                            SELECT 1 FROM fp_150 fr
                             WHERE LOWER(fr.status) IN """ + ACCEPTED_STATUSES + """
                               AND ((fr.username = l.username AND fr.friend_name = :username)
                                 OR (fr.username = :username AND fr.friend_name = l.username))
                        )
                   )
                   AND NOT EXISTS (
                       SELECT 1 FROM fp_160 b
                        WHERE (b.blocker_username = l.username AND b.blocked_username = :username)
                           OR (b.blocker_username = :username AND b.blocked_username = l.username)
                   )
                   AND NOT EXISTS (
                       SELECT 1 FROM fp_40 r
                        WHERE r.reporter_username = :username
                          AND UPPER(BTRIM(r.target_flag)) = 'Y'
                          AND r.unflagged_at IS NULL
                          AND LOWER(r.target_type) = 'user'
                          AND (r.target_username = l.username
                            OR r.target_user_id = liker.user_id
                            OR r.target_id = liker.user_id)
                   )
                   AND NOT EXISTS (
                       SELECT 1 FROM fp_40 r
                        WHERE r.reporter_username = l.username
                          AND UPPER(BTRIM(r.target_flag)) = 'Y'
                          AND r.unflagged_at IS NULL
                          AND ((LOWER(r.target_type) = 'image' AND r.target_id = f.feed_no)
                            OR (LOWER(r.target_type) = 'user' AND (
                                r.target_username = :username
                                OR r.target_user_id = owner.user_id
                                OR r.target_id = owner.user_id
                            )))
                   )
            ),
            accepted_counterparts AS (
                SELECT DISTINCT CASE WHEN f.username = :username THEN f.friend_name ELSE f.username END AS counterpart
                  FROM fp_150 f
                 WHERE LOWER(f.status) IN """ + ACCEPTED_STATUSES + """
                   AND (f.username = :username OR f.friend_name = :username)
            ),
            accepted_friends AS (
                SELECT a.counterpart
                  FROM accepted_counterparts a
                  JOIN fp_100 u ON u.username = a.counterpart
                 WHERE a.counterpart IS NOT NULL
                   AND a.counterpart <> :username
                   AND NOT EXISTS (
                       SELECT 1 FROM fp_160 b
                        WHERE (b.blocker_username = :username AND b.blocked_username = a.counterpart)
                           OR (b.blocker_username = a.counterpart AND b.blocked_username = :username)
                   )
            ),
            pending_candidates AS (
                SELECT DISTINCT f.initiator_username AS requester
                  FROM fp_150 f
                 WHERE LOWER(f.status) IN """ + PENDING_STATUSES + """
                   AND f.initiator_username IS NOT NULL
                   AND (
                       (f.initiator_username = f.username AND f.friend_name = :username)
                       OR (f.initiator_username = f.friend_name AND f.username = :username)
                   )
            ),
            pending_requests AS (
                SELECT p.requester
                  FROM pending_candidates p
                  JOIN fp_100 u ON u.username = p.requester
                 WHERE p.requester IS NOT NULL
                   AND p.requester <> :username
                   AND NOT EXISTS (
                       SELECT 1 FROM fp_160 b
                        WHERE (b.blocker_username = :username AND b.blocked_username = p.requester)
                           OR (b.blocker_username = p.requester AND b.blocked_username = :username)
                   )
            )
            SELECT
                (SELECT COUNT(*) FROM fp_300 v
                  WHERE v.username = :username AND v.use_yn = 'Y' AND v.deleted_at IS NULL
                    AND v.file_name IS NOT NULL AND BTRIM(v.file_name) <> '') AS video_count,
                (SELECT COUNT(*) FROM fp_400 f
                  WHERE f.username = :username AND f.use_yn = 'Y'
                    AND f.images IS NOT NULL AND BTRIM(f.images) <> '') AS image_count,
                (SELECT COUNT(*) FROM visible_liked) AS liked_content_count,
                (SELECT COUNT(*) FROM valid_received) AS received_like_count,
                (SELECT COUNT(*) FROM accepted_friends) AS friend_count,
                (SELECT COUNT(*) FROM pending_requests) AS pending_friend_request_count
            """;

    private static final String PLACE_CTE = """
            WITH place_ranked AS (
                SELECT p.place_id, p.formatted_address, p.latitude, p.longitude,
                       ROW_NUMBER() OVER (PARTITION BY p.place_id ORDER BY p.id DESC) AS row_num
                  FROM fp_310 p
                 WHERE p.place_id IS NOT NULL
                   AND p.use_yn = 'Y'
                   AND p.deleted_at IS NULL
            ),
            place_latest AS (
                SELECT place_id, formatted_address, latitude, longitude
                  FROM place_ranked
                 WHERE row_num = 1
            )
            """;

    private static final String RECENT_CONTENT_SQL = PLACE_CTE + """
            SELECT *
              FROM (
                    SELECT 'VIDEO' AS content_type, 0 AS type_rank, v.store_id AS source_id,
                           v.place_id, v.title, v.thumbnail AS thumbnail_source,
                           CAST(NULL AS TEXT) AS images_source, v.store_name,
                           COALESCE(NULLIF(BTRIM(v.address), ''), p.formatted_address) AS address,
                           p.latitude, p.longitude, v.username AS author_username,
                           COALESCE(NULLIF(BTRIM(u.nick_name), ''), u.username) AS author_display_name,
                           u.profile_image_url AS author_profile_image_url,
                           v.created_at AS created_on, CAST(NULL AS DATE) AS liked_on,
                           v.created_at AS sort_date
                      FROM fp_300 v
                      JOIN fp_100 u ON u.username = v.username
                      LEFT JOIN place_latest p ON p.place_id = v.place_id
                     WHERE v.username = :username
                       AND v.use_yn = 'Y'
                       AND v.deleted_at IS NULL
                       AND v.file_name IS NOT NULL
                       AND BTRIM(v.file_name) <> ''
                    UNION ALL
                    SELECT 'IMAGE' AS content_type, 1 AS type_rank, f.feed_no AS source_id,
                           f.place_id, f.feed_title AS title, f.thumbnail AS thumbnail_source,
                           f.images AS images_source, f.store_name,
                           COALESCE(NULLIF(BTRIM(f.location), ''), p.formatted_address) AS address,
                           p.latitude, p.longitude, f.username AS author_username,
                           COALESCE(NULLIF(BTRIM(u.nick_name), ''), u.username) AS author_display_name,
                           u.profile_image_url AS author_profile_image_url,
                           CAST(f.created_at AS DATE) AS created_on, CAST(NULL AS DATE) AS liked_on,
                           CAST(f.created_at AS DATE) AS sort_date
                      FROM fp_400 f
                      JOIN fp_100 u ON u.username = f.username
                      LEFT JOIN place_latest p ON p.place_id = f.place_id
                     WHERE f.username = :username
                       AND f.use_yn = 'Y'
                       AND f.images IS NOT NULL
                       AND BTRIM(f.images) <> ''
              ) items
             ORDER BY sort_date DESC NULLS LAST, type_rank ASC, source_id DESC
             LIMIT :limit
            """;

    private static final String RECENT_LIKES_SQL = PLACE_CTE + """
            SELECT *
              FROM (
                    SELECT 'VIDEO' AS content_type, 0 AS type_rank, v.store_id AS source_id,
                           v.place_id, v.title, v.thumbnail AS thumbnail_source,
                           CAST(NULL AS TEXT) AS images_source, v.store_name,
                           COALESCE(NULLIF(BTRIM(v.address), ''), p.formatted_address) AS address,
                           p.latitude, p.longitude, v.username AS author_username,
                           COALESCE(NULLIF(BTRIM(author.nick_name), ''), author.username) AS author_display_name,
                           author.profile_image_url AS author_profile_image_url,
                           v.created_at AS created_on,
                           CAST(COALESCE(l.updated_at, l.created_at) AS DATE) AS liked_on,
                           CAST(COALESCE(l.updated_at, l.created_at) AS DATE) AS sort_date
                      FROM fp_50 l
                      JOIN fp_300 v ON v.store_id = l.store_id
                      JOIN fp_100 author ON author.username = v.username
                      LEFT JOIN place_latest p ON p.place_id = v.place_id
                     WHERE l.username = :username
                       AND l.use_yn = 'Y'
                       AND l.deleted_at IS NULL
                       AND v.use_yn = 'Y'
                       AND v.deleted_at IS NULL
                       AND v.file_name IS NOT NULL
                       AND BTRIM(v.file_name) <> ''
                       AND (v.username = :username OR (
                            v.open_yn = 'Y'
                            AND (COALESCE(author.is_private, TRUE) = FALSE OR EXISTS (
                                SELECT 1 FROM fp_150 fr
                                 WHERE LOWER(fr.status) IN """ + ACCEPTED_STATUSES + """
                                   AND ((fr.username = :username AND fr.friend_name = v.username)
                                     OR (fr.username = v.username AND fr.friend_name = :username))
                            ))
                       ))
                       AND NOT EXISTS (
                           SELECT 1 FROM fp_160 b
                            WHERE (b.blocker_username = :username AND b.blocked_username = v.username)
                               OR (b.blocker_username = v.username AND b.blocked_username = :username)
                       )
                       AND NOT EXISTS (
                           SELECT 1 FROM fp_40 r
                            WHERE r.reporter_username = :username
                              AND UPPER(BTRIM(r.target_flag)) = 'Y'
                              AND r.unflagged_at IS NULL
                              AND ((LOWER(r.target_type) = 'video' AND r.target_id = v.store_id)
                                OR (LOWER(r.target_type) = 'user' AND (
                                    r.target_username = v.username
                                    OR r.target_user_id = author.user_id
                                    OR r.target_id = author.user_id
                                )))
                       )
                    UNION ALL
                    SELECT 'IMAGE' AS content_type, 1 AS type_rank, f.feed_no AS source_id,
                           f.place_id, f.feed_title AS title, f.thumbnail AS thumbnail_source,
                           f.images AS images_source, f.store_name,
                           COALESCE(NULLIF(BTRIM(f.location), ''), p.formatted_address) AS address,
                           p.latitude, p.longitude, f.username AS author_username,
                           COALESCE(NULLIF(BTRIM(author.nick_name), ''), author.username) AS author_display_name,
                           author.profile_image_url AS author_profile_image_url,
                           CAST(f.created_at AS DATE) AS created_on,
                           CAST(COALESCE(l.updated_at, l.created_at) AS DATE) AS liked_on,
                           CAST(COALESCE(l.updated_at, l.created_at) AS DATE) AS sort_date
                      FROM fp_60 l
                      JOIN fp_400 f ON f.feed_no = l.feed_id
                      JOIN fp_100 author ON author.username = f.username
                      LEFT JOIN place_latest p ON p.place_id = f.place_id
                     WHERE l.username = :username
                       AND l.use_yn = 'Y'
                       AND l.deleted_at IS NULL
                       AND f.use_yn = 'Y'
                       AND f.images IS NOT NULL
                       AND BTRIM(f.images) <> ''
                       AND (f.username = :username OR (
                            f.open_yn = 'Y'
                            AND (COALESCE(author.is_private, TRUE) = FALSE OR EXISTS (
                                SELECT 1 FROM fp_150 fr
                                 WHERE LOWER(fr.status) IN """ + ACCEPTED_STATUSES + """
                                   AND ((fr.username = :username AND fr.friend_name = f.username)
                                     OR (fr.username = f.username AND fr.friend_name = :username))
                            ))
                       ))
                       AND NOT EXISTS (
                           SELECT 1 FROM fp_160 b
                            WHERE (b.blocker_username = :username AND b.blocked_username = f.username)
                               OR (b.blocker_username = f.username AND b.blocked_username = :username)
                       )
                       AND NOT EXISTS (
                           SELECT 1 FROM fp_40 r
                            WHERE r.reporter_username = :username
                              AND UPPER(BTRIM(r.target_flag)) = 'Y'
                              AND r.unflagged_at IS NULL
                              AND ((LOWER(r.target_type) = 'image' AND r.target_id = f.feed_no)
                                OR (LOWER(r.target_type) = 'user' AND (
                                    r.target_username = f.username
                                    OR r.target_user_id = author.user_id
                                    OR r.target_id = author.user_id
                                )))
                       )
              ) items
             ORDER BY sort_date DESC NULLS LAST, type_rank ASC, source_id DESC
             LIMIT :limit
            """;
}
