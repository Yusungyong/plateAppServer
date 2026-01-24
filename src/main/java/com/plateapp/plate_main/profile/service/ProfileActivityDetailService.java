package com.plateapp.plate_main.profile.service;

import com.plateapp.plate_main.profile.dto.LikedContentDtos.LikedImageItem;
import com.plateapp.plate_main.profile.dto.LikedContentDtos.LikedVideoItem;
import com.plateapp.plate_main.profile.dto.ProfileActivityDetailItems.ImageItem;
import com.plateapp.plate_main.profile.dto.ProfileActivityDetailItems.VideoItem;
import com.plateapp.plate_main.profile.dto.ProfileActivityDetailResponse;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileActivityDetailService {

    private static final int LIMIT_MAX = 100;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private record LikedEntry(Integer id, LocalDateTime likedAt) {}

    @Transactional(readOnly = true)
    public ProfileActivityDetailResponse<VideoItem> getUserVideos(
            String username,
            int limit,
            int offset,
            String sort,
            LocalDate from,
            LocalDate to,
            String region
    ) {
        int safeLimit = clampLimit(limit);
        int safeOffset = Math.max(0, offset);
        LocalDateTime fromTs = toStartOfDay(from);
        LocalDateTime toTs = toEndExclusive(to);
        String regionValue = normalizeRegion(region);

        MapSqlParameterSource params = baseParams(username, safeLimit, safeOffset);
        List<Integer> storeIds = fetchUserVideoIds(params, isPopular(sort), fromTs, toTs, regionValue);
        List<VideoItem> items = storeIds.isEmpty()
                ? Collections.emptyList()
                : buildVideoItems(storeIds, loadVideoDetails(storeIds), loadVideoLikeCounts(storeIds), loadVideoCommentCounts(storeIds));

        long total = countUserVideos(username, fromTs, toTs, regionValue);
        return ProfileActivityDetailResponse.<VideoItem>builder()
                .items(items)
                .limit(safeLimit)
                .offset(safeOffset)
                .total(total)
                .build();
    }

    @Transactional(readOnly = true)
    public ProfileActivityDetailResponse<ImageItem> getUserImages(
            String username,
            int limit,
            int offset,
            String sort,
            LocalDate from,
            LocalDate to,
            String region
    ) {
        int safeLimit = clampLimit(limit);
        int safeOffset = Math.max(0, offset);
        LocalDateTime fromTs = toStartOfDay(from);
        LocalDateTime toTs = toEndExclusive(to);
        String regionValue = normalizeRegion(region);

        MapSqlParameterSource params = baseParams(username, safeLimit, safeOffset);
        List<Integer> feedIds = fetchUserImageIds(params, isPopular(sort), fromTs, toTs, regionValue);
        List<ImageItem> items = feedIds.isEmpty()
                ? Collections.emptyList()
                : buildImageItems(feedIds, loadImageDetails(feedIds), loadImageLikeCounts(feedIds), loadImageCommentCounts(feedIds));

        long total = countUserImages(username, fromTs, toTs, regionValue);
        return ProfileActivityDetailResponse.<ImageItem>builder()
                .items(items)
                .limit(safeLimit)
                .offset(safeOffset)
                .total(total)
                .build();
    }

    @Transactional(readOnly = true)
    public ProfileActivityDetailResponse<LikedVideoItem> getLikedVideos(
            String username,
            int limit,
            int offset,
            String sort,
            LocalDate from,
            LocalDate to,
            String region
    ) {
        int safeLimit = clampLimit(limit);
        int safeOffset = Math.max(0, offset);
        LocalDateTime fromTs = toStartOfDay(from);
        LocalDateTime toTs = toEndExclusive(to);
        String regionValue = normalizeRegion(region);

        MapSqlParameterSource params = baseParams(username, safeLimit, safeOffset);
        List<LikedEntry> likedEntries = fetchLikedVideoEntries(params, isPopular(sort), fromTs, toTs, regionValue);
        List<LikedVideoItem> items = likedEntries.isEmpty()
                ? Collections.emptyList()
                : buildLikedVideoItems(likedEntries, loadVideoDetails(idsFromLiked(likedEntries)));

        long total = countLikedVideos(username, fromTs, toTs, regionValue);
        return ProfileActivityDetailResponse.<LikedVideoItem>builder()
                .items(items)
                .limit(safeLimit)
                .offset(safeOffset)
                .total(total)
                .build();
    }

    @Transactional(readOnly = true)
    public ProfileActivityDetailResponse<LikedImageItem> getLikedImages(
            String username,
            int limit,
            int offset,
            String sort,
            LocalDate from,
            LocalDate to,
            String region
    ) {
        int safeLimit = clampLimit(limit);
        int safeOffset = Math.max(0, offset);
        LocalDateTime fromTs = toStartOfDay(from);
        LocalDateTime toTs = toEndExclusive(to);
        String regionValue = normalizeRegion(region);

        MapSqlParameterSource params = baseParams(username, safeLimit, safeOffset);
        List<LikedEntry> likedEntries = fetchLikedImageEntries(params, isPopular(sort), fromTs, toTs, regionValue);
        List<LikedImageItem> items = likedEntries.isEmpty()
                ? Collections.emptyList()
                : buildLikedImageItems(likedEntries, loadImageDetails(idsFromLiked(likedEntries)));

        long total = countLikedImages(username, fromTs, toTs, regionValue);
        return ProfileActivityDetailResponse.<LikedImageItem>builder()
                .items(items)
                .limit(safeLimit)
                .offset(safeOffset)
                .total(total)
                .build();
    }

    private List<Integer> fetchUserVideoIds(
            MapSqlParameterSource params,
            boolean popular,
            LocalDateTime fromTs,
            LocalDateTime toTs,
            String regionValue
    ) {
        String orderBy = popular
                ? "ORDER BY COALESCE(l.like_count, 0) DESC, COALESCE(c.comment_count, 0) DESC, s.created_at DESC, s.store_id DESC"
                : "ORDER BY s.created_at DESC NULLS LAST, s.store_id DESC";
        String joinCounts = popular
                ? """
                    LEFT JOIN (
                      SELECT store_id, COUNT(*) AS like_count
                      FROM fp_50
                      WHERE use_yn = 'Y' AND deleted_at IS NULL
                      GROUP BY store_id
                    ) l ON l.store_id = s.store_id
                    LEFT JOIN (
                      SELECT store_id, COUNT(*) AS comment_count
                      FROM fp_440
                      WHERE use_yn = 'Y' AND deleted_at IS NULL
                      GROUP BY store_id
                    ) c ON c.store_id = s.store_id
                    """
                : "";
        StringBuilder sql = new StringBuilder("""
            SELECT s.store_id
            FROM fp_300 s
            """).append(joinCounts).append("""
            WHERE s.username = :username
              AND s.use_yn = 'Y'
              AND s.open_yn = 'Y'
              AND s.deleted_at IS NULL
            """);
        appendDateFilter(sql, params, "s.created_at", fromTs, toTs);
        appendVideoRegionFilter(sql, params, regionValue);
        sql.append("\n").append(orderBy).append("\nLIMIT :limit OFFSET :offset");

        return jdbcTemplate.query(sql.toString(), params, (rs, rowNum) -> rs.getInt("store_id"));
    }

    private List<Integer> fetchUserImageIds(
            MapSqlParameterSource params,
            boolean popular,
            LocalDateTime fromTs,
            LocalDateTime toTs,
            String regionValue
    ) {
        String orderBy = popular
                ? "ORDER BY COALESCE(l.like_count, 0) DESC, COALESCE(c.comment_count, 0) DESC, f.created_at DESC, f.feed_no DESC"
                : "ORDER BY f.created_at DESC NULLS LAST, f.feed_no DESC";
        String joinCounts = popular
                ? """
                    LEFT JOIN (
                      SELECT feed_id, COUNT(*) AS like_count
                      FROM fp_60
                      WHERE use_yn = 'Y' AND deleted_at IS NULL
                      GROUP BY feed_id
                    ) l ON l.feed_id = f.feed_no
                    LEFT JOIN (
                      SELECT feed_id, COUNT(*) AS comment_count
                      FROM fp_460
                      WHERE use_yn = 'Y' AND deleted_at IS NULL
                      GROUP BY feed_id
                    ) c ON c.feed_id = f.feed_no
                    """
                : "";
        StringBuilder sql = new StringBuilder("""
            SELECT f.feed_no AS feed_id
            FROM fp_400 f
            """).append(joinCounts).append("""
            WHERE f.username = :username
              AND f.use_yn = 'Y'
            """);
        appendDateFilter(sql, params, "f.created_at", fromTs, toTs);
        appendImageRegionFilter(sql, params, regionValue);
        sql.append("\n").append(orderBy).append("\nLIMIT :limit OFFSET :offset");

        return jdbcTemplate.query(sql.toString(), params, (rs, rowNum) -> rs.getInt("feed_id"));
    }

    private List<LikedEntry> fetchLikedVideoEntries(
            MapSqlParameterSource params,
            boolean popular,
            LocalDateTime fromTs,
            LocalDateTime toTs,
            String regionValue
    ) {
        String orderBy = popular
                ? "ORDER BY COALESCE(lc.like_count, 0) DESC, l.created_at DESC, s.store_id DESC"
                : "ORDER BY l.created_at DESC NULLS LAST, s.store_id DESC";
        String joinCounts = popular
                ? """
                    LEFT JOIN (
                      SELECT store_id, COUNT(*) AS like_count
                      FROM fp_50
                      WHERE use_yn = 'Y' AND deleted_at IS NULL
                      GROUP BY store_id
                    ) lc ON lc.store_id = s.store_id
                    """
                : "";
        StringBuilder sql = new StringBuilder("""
            SELECT l.store_id, l.created_at AS liked_at
            FROM fp_50 l
            JOIN fp_300 s ON s.store_id = l.store_id
            """).append(joinCounts).append("""
            WHERE l.username = :username
              AND l.use_yn = 'Y'
              AND l.deleted_at IS NULL
              AND s.use_yn = 'Y'
              AND s.open_yn = 'Y'
              AND s.deleted_at IS NULL
            """);
        appendDateFilter(sql, params, "l.created_at", fromTs, toTs);
        appendVideoRegionFilter(sql, params, regionValue);
        sql.append("\n").append(orderBy).append("\nLIMIT :limit OFFSET :offset");

        return jdbcTemplate.query(sql.toString(), params, (rs, rowNum) -> new LikedEntry(
                rs.getInt("store_id"),
                toLocalDateTime(rs.getTimestamp("liked_at"))
        ));
    }

    private List<LikedEntry> fetchLikedImageEntries(
            MapSqlParameterSource params,
            boolean popular,
            LocalDateTime fromTs,
            LocalDateTime toTs,
            String regionValue
    ) {
        String orderBy = popular
                ? "ORDER BY COALESCE(lc.like_count, 0) DESC, l.created_at DESC, f.feed_no DESC"
                : "ORDER BY l.created_at DESC NULLS LAST, f.feed_no DESC";
        String joinCounts = popular
                ? """
                    LEFT JOIN (
                      SELECT feed_id, COUNT(*) AS like_count
                      FROM fp_60
                      WHERE use_yn = 'Y' AND deleted_at IS NULL
                      GROUP BY feed_id
                    ) lc ON lc.feed_id = f.feed_no
                    """
                : "";
        StringBuilder sql = new StringBuilder("""
            SELECT f.feed_no AS feed_id, l.created_at AS liked_at
            FROM fp_60 l
            JOIN fp_400 f ON f.feed_no = l.feed_id
            """).append(joinCounts).append("""
            WHERE l.username = :username
              AND l.use_yn = 'Y'
              AND l.deleted_at IS NULL
              AND f.use_yn = 'Y'
            """);
        appendDateFilter(sql, params, "l.created_at", fromTs, toTs);
        appendImageRegionFilter(sql, params, regionValue);
        sql.append("\n").append(orderBy).append("\nLIMIT :limit OFFSET :offset");

        return jdbcTemplate.query(sql.toString(), params, (rs, rowNum) -> new LikedEntry(
                rs.getInt("feed_id"),
                toLocalDateTime(rs.getTimestamp("liked_at"))
        ));
    }

    private Map<Integer, VideoItem> loadVideoDetails(List<Integer> storeIds) {
        if (storeIds.isEmpty()) {
            return Collections.emptyMap();
        }
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
            WHERE s.store_id IN (:ids)
            """;
        MapSqlParameterSource params = new MapSqlParameterSource("ids", storeIds);
        Map<Integer, VideoItem> map = new HashMap<>();
        jdbcTemplate.query(sql, params, (rs, rowNum) -> {
            VideoItem item = new VideoItem(
                    rs.getInt("store_id"),
                    rs.getString("title"),
                    rs.getString("thumbnail"),
                    rs.getString("file_name"),
                    (Integer) rs.getObject("video_duration"),
                    rs.getString("place_id"),
                    rs.getString("store_name"),
                    rs.getString("address"),
                    toLocalDateTime(rs.getTimestamp("created_at")),
                    toLocalDateTime(rs.getTimestamp("updated_at")),
                    0L,
                    0L
            );
            map.put(item.storeId(), item);
            return item;
        });
        return map;
    }

    private Map<Integer, ImageItem> loadImageDetails(List<Integer> feedIds) {
        if (feedIds.isEmpty()) {
            return Collections.emptyMap();
        }
        String sql = """
            SELECT
              f.feed_no AS feed_id,
              COALESCE(f.feed_title, f.store_name) AS title,
              f.thumbnail,
              f.images,
              f.place_id,
              f.store_name,
              f.location,
              f.created_at,
              f.updated_at
            FROM fp_400 f
            WHERE f.feed_no IN (:ids)
            """;
        MapSqlParameterSource params = new MapSqlParameterSource("ids", feedIds);
        Map<Integer, ImageItem> map = new HashMap<>();
        jdbcTemplate.query(sql, params, (rs, rowNum) -> {
            String thumbnail = rs.getString("thumbnail");
            String images = rs.getString("images");
            int imageCount = 0;
            if (images != null && !images.isBlank()) {
                String[] arr = images.split(",");
                imageCount = arr.length;
                if (thumbnail == null || thumbnail.isBlank()) {
                    thumbnail = arr[0].trim();
                }
            }

            ImageItem item = new ImageItem(
                    rs.getInt("feed_id"),
                    rs.getString("title"),
                    thumbnail,
                    rs.getString("place_id"),
                    rs.getString("store_name"),
                    rs.getString("location"),
                    toLocalDateTime(rs.getTimestamp("created_at")),
                    toLocalDateTime(rs.getTimestamp("updated_at")),
                    imageCount,
                    0L,
                    0L
            );
            map.put(item.feedId(), item);
            return item;
        });
        return map;
    }

    private Map<Integer, Long> loadVideoLikeCounts(List<Integer> storeIds) {
        return loadCounts("fp_50", "store_id", storeIds);
    }

    private Map<Integer, Long> loadVideoCommentCounts(List<Integer> storeIds) {
        return loadCounts("fp_440", "store_id", storeIds);
    }

    private Map<Integer, Long> loadImageLikeCounts(List<Integer> feedIds) {
        return loadCounts("fp_60", "feed_id", feedIds);
    }

    private Map<Integer, Long> loadImageCommentCounts(List<Integer> feedIds) {
        return loadCounts("fp_460", "feed_id", feedIds);
    }

    private Map<Integer, Long> loadCounts(String table, String idColumn, List<Integer> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        String sql = """
            SELECT %s AS id, COUNT(*) AS cnt
            FROM %s
            WHERE use_yn = 'Y' AND deleted_at IS NULL
              AND %s IN (:ids)
            GROUP BY %s
            """.formatted(idColumn, table, idColumn, idColumn);
        MapSqlParameterSource params = new MapSqlParameterSource("ids", ids);
        Map<Integer, Long> map = new HashMap<>();
        jdbcTemplate.query(sql, params, (rs, rowNum) -> {
            map.put(rs.getInt("id"), rs.getLong("cnt"));
            return null;
        });
        return map;
    }

    private List<VideoItem> buildVideoItems(
            List<Integer> storeIds,
            Map<Integer, VideoItem> details,
            Map<Integer, Long> likeCounts,
            Map<Integer, Long> commentCounts
    ) {
        List<VideoItem> items = new ArrayList<>(storeIds.size());
        for (Integer storeId : storeIds) {
            VideoItem base = details.get(storeId);
            if (base == null) {
                continue;
            }
            items.add(new VideoItem(
                    base.storeId(),
                    base.title(),
                    base.thumbnail(),
                    base.fileName(),
                    base.videoDuration(),
                    base.placeId(),
                    base.storeName(),
                    base.address(),
                    base.createdAt(),
                    base.updatedAt(),
                    likeCounts.getOrDefault(storeId, 0L),
                    commentCounts.getOrDefault(storeId, 0L)
            ));
        }
        return items;
    }

    private List<ImageItem> buildImageItems(
            List<Integer> feedIds,
            Map<Integer, ImageItem> details,
            Map<Integer, Long> likeCounts,
            Map<Integer, Long> commentCounts
    ) {
        List<ImageItem> items = new ArrayList<>(feedIds.size());
        for (Integer feedId : feedIds) {
            ImageItem base = details.get(feedId);
            if (base == null) {
                continue;
            }
            items.add(new ImageItem(
                    base.feedId(),
                    base.title(),
                    base.thumbnail(),
                    base.placeId(),
                    base.storeName(),
                    base.address(),
                    base.createdAt(),
                    base.updatedAt(),
                    base.imageCount(),
                    likeCounts.getOrDefault(feedId, 0L),
                    commentCounts.getOrDefault(feedId, 0L)
            ));
        }
        return items;
    }

    private List<LikedVideoItem> buildLikedVideoItems(
            List<LikedEntry> likedEntries,
            Map<Integer, VideoItem> details
    ) {
        List<LikedVideoItem> items = new ArrayList<>(likedEntries.size());
        for (LikedEntry entry : likedEntries) {
            VideoItem base = details.get(entry.id());
            if (base == null) {
                continue;
            }
            items.add(new LikedVideoItem(
                    base.storeId(),
                    base.title(),
                    base.thumbnail(),
                    base.fileName(),
                    base.videoDuration(),
                    base.placeId(),
                    base.storeName(),
                    base.address(),
                    entry.likedAt()
            ));
        }
        return items;
    }

    private List<LikedImageItem> buildLikedImageItems(
            List<LikedEntry> likedEntries,
            Map<Integer, ImageItem> details
    ) {
        List<LikedImageItem> items = new ArrayList<>(likedEntries.size());
        for (LikedEntry entry : likedEntries) {
            ImageItem base = details.get(entry.id());
            if (base == null) {
                continue;
            }
            items.add(new LikedImageItem(
                    base.feedId(),
                    base.title(),
                    base.thumbnail(),
                    base.placeId(),
                    base.storeName(),
                    entry.likedAt()
            ));
        }
        return items;
    }

    private long countUserVideos(String username, LocalDateTime fromTs, LocalDateTime toTs, String regionValue) {
        MapSqlParameterSource params = new MapSqlParameterSource("username", username);
        StringBuilder sql = new StringBuilder("""
            SELECT COUNT(*)
            FROM fp_300 s
            WHERE s.username = :username
              AND s.use_yn = 'Y'
              AND s.open_yn = 'Y'
              AND s.deleted_at IS NULL
            """);
        appendDateFilter(sql, params, "s.created_at", fromTs, toTs);
        appendVideoRegionFilter(sql, params, regionValue);
        return jdbcTemplate.queryForObject(sql.toString(), params, Long.class);
    }

    private long countUserImages(String username, LocalDateTime fromTs, LocalDateTime toTs, String regionValue) {
        MapSqlParameterSource params = new MapSqlParameterSource("username", username);
        StringBuilder sql = new StringBuilder("""
            SELECT COUNT(*)
            FROM fp_400 f
            WHERE f.username = :username
              AND f.use_yn = 'Y'
            """);
        appendDateFilter(sql, params, "f.created_at", fromTs, toTs);
        appendImageRegionFilter(sql, params, regionValue);
        return jdbcTemplate.queryForObject(sql.toString(), params, Long.class);
    }

    private long countLikedVideos(String username, LocalDateTime fromTs, LocalDateTime toTs, String regionValue) {
        MapSqlParameterSource params = new MapSqlParameterSource("username", username);
        StringBuilder sql = new StringBuilder("""
            SELECT COUNT(*)
            FROM fp_50 l
            JOIN fp_300 s ON s.store_id = l.store_id
            WHERE l.username = :username
              AND l.use_yn = 'Y'
              AND l.deleted_at IS NULL
              AND s.use_yn = 'Y'
              AND s.open_yn = 'Y'
              AND s.deleted_at IS NULL
            """);
        appendDateFilter(sql, params, "l.created_at", fromTs, toTs);
        appendVideoRegionFilter(sql, params, regionValue);
        return jdbcTemplate.queryForObject(sql.toString(), params, Long.class);
    }

    private long countLikedImages(String username, LocalDateTime fromTs, LocalDateTime toTs, String regionValue) {
        MapSqlParameterSource params = new MapSqlParameterSource("username", username);
        StringBuilder sql = new StringBuilder("""
            SELECT COUNT(*)
            FROM fp_60 l
            JOIN fp_400 f ON f.feed_no = l.feed_id
            WHERE l.username = :username
              AND l.use_yn = 'Y'
              AND l.deleted_at IS NULL
              AND f.use_yn = 'Y'
            """);
        appendDateFilter(sql, params, "l.created_at", fromTs, toTs);
        appendImageRegionFilter(sql, params, regionValue);
        return jdbcTemplate.queryForObject(sql.toString(), params, Long.class);
    }

    private static void appendDateFilter(
            StringBuilder sql,
            MapSqlParameterSource params,
            String column,
            LocalDateTime fromTs,
            LocalDateTime toTs
    ) {
        if (fromTs != null) {
            sql.append(" AND ").append(column).append(" >= :fromTs");
            params.addValue("fromTs", fromTs);
        }
        if (toTs != null) {
            sql.append(" AND ").append(column).append(" < :toTs");
            params.addValue("toTs", toTs);
        }
    }

    private static void appendVideoRegionFilter(
            StringBuilder sql,
            MapSqlParameterSource params,
            String regionValue
    ) {
        if (regionValue != null) {
            sql.append(" AND (COALESCE(s.address, '') ILIKE CONCAT('%', :region, '%')")
               .append(" OR COALESCE(s.store_name, '') ILIKE CONCAT('%', :region, '%'))");
            params.addValue("region", regionValue);
        }
    }

    private static void appendImageRegionFilter(
            StringBuilder sql,
            MapSqlParameterSource params,
            String regionValue
    ) {
        if (regionValue != null) {
            sql.append(" AND (COALESCE(f.location, '') ILIKE CONCAT('%', :region, '%')")
               .append(" OR COALESCE(f.store_name, '') ILIKE CONCAT('%', :region, '%'))");
            params.addValue("region", regionValue);
        }
    }

    private static List<Integer> idsFromLiked(List<LikedEntry> entries) {
        List<Integer> ids = new ArrayList<>(entries.size());
        for (LikedEntry entry : entries) {
            ids.add(entry.id());
        }
        return ids;
    }

    private static MapSqlParameterSource baseParams(String username, int limit, int offset) {
        return new MapSqlParameterSource()
                .addValue("username", username)
                .addValue("limit", limit)
                .addValue("offset", offset);
    }

    private static String normalizeRegion(String region) {
        if (region == null || region.isBlank()) {
            return null;
        }
        return region.trim();
    }

    private static LocalDateTime toStartOfDay(LocalDate date) {
        return date != null ? date.atStartOfDay() : null;
    }

    private static LocalDateTime toEndExclusive(LocalDate date) {
        return date != null ? date.plusDays(1).atStartOfDay() : null;
    }

    private boolean isPopular(String sort) {
        return sort != null && "popular".equalsIgnoreCase(sort);
    }

    private int clampLimit(int limit) {
        if (limit < 1) {
            return 1;
        }
        return Math.min(limit, LIMIT_MAX);
    }

    private static LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts != null ? ts.toLocalDateTime() : null;
    }
}
