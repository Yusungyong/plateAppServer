package com.plateapp.plate_main.search.repository;

import com.plateapp.plate_main.map.dto.MapSearchSuggestionDto;
import com.plateapp.plate_main.search.dto.SearchItem;
import com.plateapp.plate_main.search.dto.SearchSort;
import com.plateapp.plate_main.search.dto.SearchType;
import com.plateapp.plate_main.search.service.SearchPage;
import com.plateapp.plate_main.search.service.SearchQuery;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.StringJoiner;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SearchRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public List<MapSearchSuggestionDto> suggestPlaces(String keyword, int limit) {
        String sql = """
            SELECT DISTINCT ON (s.store_name, s.place_id)
              s.place_id,
              s.store_name,
              s.address,
              loc.latitude  AS lat,
              loc.longitude AS lng
            FROM fp_300 s
            JOIN fp_310 loc ON loc.place_id = s.place_id
            WHERE s.use_yn = 'Y'
              AND s.open_yn = 'Y'
              AND s.deleted_at IS NULL
              AND (
                s.store_name ILIKE :kw
                OR s.address ILIKE :kw
              )
            ORDER BY s.store_name, s.place_id, s.store_id DESC
            LIMIT :limit
            """;
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("kw", "%" + keyword + "%")
            .addValue("limit", limit);
        return jdbc.query(sql, params, (rs, rowNum) -> new MapSearchSuggestionDto(
            rs.getString("place_id"),
            rs.getString("store_name"),
            rs.getString("address"),
            rs.getObject("lat", Double.class),
            rs.getObject("lng", Double.class)
        ));
    }

    public List<String> suggestTags(String keyword, int limit) {
        String sql = """
            SELECT DISTINCT trim(tag) AS tag
            FROM fp_350 t,
                 regexp_split_to_table(t.tags, ',') AS tag
            WHERE tag ILIKE :kw
              AND trim(tag) <> ''
            ORDER BY tag
            LIMIT :limit
            """;
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("kw", "%" + keyword + "%")
            .addValue("limit", limit);
        return jdbc.query(sql, params, (rs, rowNum) -> rs.getString("tag"));
    }

    public SearchPage search(SearchQuery query) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String keyword = query.keyword();
        String category = query.category();
        List<String> tags = query.tags();
        Double lat = query.lat();
        Double lng = query.lng();
        Integer radiusM = query.radiusM();
        int limit = query.size();
        int offset = Math.max(0, query.page()) * limit;

        if (keyword != null && !keyword.isBlank()) {
            params.addValue("kw", "%" + keyword.trim() + "%");
        }
        if (category != null && !category.isBlank()) {
            params.addValue("category", "%" + category.trim() + "%");
        }
        if (lat != null && lng != null) {
            params.addValue("lat", lat).addValue("lng", lng);
        }
        if (radiusM != null) {
            params.addValue("radius_m", radiusM);
        }
        params.addValue("limit", limit);
        params.addValue("offset", offset);

        String distanceExpr = buildDistanceExpr(lat, lng);
        String placeSql = buildPlaceQuery(keyword, category, tags, lat, lng, radiusM, distanceExpr, params);
        String videoSql = buildVideoQuery(keyword, category, tags, lat, lng, radiusM, distanceExpr, params);
        String imageSql = buildImageQuery(keyword, category, tags, lat, lng, radiusM, distanceExpr, params);

        String unionSql = """
            WITH feed_counts AS (
              SELECT place_id, COUNT(*)::int AS feed_count
              FROM fp_400
              WHERE use_yn = 'Y'
              GROUP BY place_id
            ),
            latest_feeds AS (
              SELECT DISTINCT ON (f.place_id)
                f.place_id,
                f.feed_no AS latest_feed_id
              FROM fp_400 f
              WHERE f.use_yn = 'Y'
              ORDER BY f.place_id, f.created_at DESC NULLS LAST, f.feed_no DESC
            ),
            unioned AS (
            %s
            )
            SELECT *
            FROM unioned
            %s
            %s
            LIMIT :limit
            OFFSET :offset
            """;

        String typeFilter = "";
        if (query.type() != null && query.type() != SearchType.ALL) {
            params.addValue("type", query.type().name().toLowerCase(Locale.US));
            typeFilter = "WHERE type = :type";
        }

        String orderBy = orderByClause(query.sort());

        String finalSql = String.format(unionSql, joinUnion(placeSql, videoSql, imageSql), typeFilter, orderBy);
        List<SearchItem> items = jdbc.query(finalSql, params, (rs, rowNum) -> {
            Double distanceRaw = rs.getObject("distance_m", Double.class);
            Integer distanceM = (distanceRaw == null) ? null : (int) Math.round(distanceRaw);
            return new SearchItem(
                rs.getString("type"),
                rs.getString("place_id"),
                rs.getObject("store_id", Integer.class),
                rs.getString("store_name"),
                rs.getString("address"),
                rs.getObject("lat", Double.class),
                rs.getObject("lng", Double.class),
                distanceM,
                rs.getObject("feed_count", Integer.class),
                rs.getString("content_type"),
                rs.getObject("image_feed_id", Integer.class),
                rs.getString("thumbnail"),
                rs.getString("title"),
                rs.getObject("feed_id", Integer.class),
                toOffset(rs.getObject("created_at"))
            );
        });

        String countSql = """
            WITH feed_counts AS (
              SELECT place_id, COUNT(*)::int AS feed_count
              FROM fp_400
              WHERE use_yn = 'Y'
              GROUP BY place_id
            ),
            latest_feeds AS (
              SELECT DISTINCT ON (f.place_id)
                f.place_id,
                f.feed_no AS latest_feed_id
              FROM fp_400 f
              WHERE f.use_yn = 'Y'
              ORDER BY f.place_id, f.created_at DESC NULLS LAST, f.feed_no DESC
            ),
            unioned AS (
            %s
            )
            SELECT COUNT(*) FROM unioned %s
            """;

        String finalCountSql = String.format(countSql, joinUnion(placeSql, videoSql, imageSql), typeFilter);
        Long total = jdbc.queryForObject(finalCountSql, params, Long.class);
        long safeTotal = (total == null) ? 0 : total;
        return new SearchPage(items, safeTotal);
    }

    private String joinUnion(String placeSql, String videoSql, String imageSql) {
        StringJoiner joiner = new StringJoiner("\nUNION ALL\n");
        joiner.add(placeSql);
        joiner.add(videoSql);
        joiner.add(imageSql);
        return joiner.toString();
    }

    private String buildPlaceQuery(
        String keyword,
        String category,
        List<String> tags,
        Double lat,
        Double lng,
        Integer radiusM,
        String distanceExpr,
        MapSqlParameterSource params
    ) {
        StringBuilder where = new StringBuilder("WHERE s.use_yn = 'Y' AND s.open_yn = 'Y' AND s.deleted_at IS NULL");
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (s.store_name ILIKE :kw OR s.address ILIKE :kw OR s.title ILIKE :kw)");
        }
        if (category != null && !category.isBlank()) {
            where.append(" AND EXISTS (SELECT 1 FROM unnest(loc.types) t WHERE t ILIKE :category)");
        }
        where.append(buildTagFilter(tags, "store_id", "s.store_id", params, "tag_place_"));
        where.append(buildRadiusFilter(lat, lng, radiusM, distanceExpr));

        return """
            SELECT
              'place' AS type,
              s.place_id,
              s.store_id,
              s.store_name,
              s.address,
              loc.latitude AS lat,
              loc.longitude AS lng,
              %s AS distance_m,
              COALESCE(fc.feed_count, 0) AS feed_count,
              CASE
                WHEN (s.file_name IS NOT NULL AND s.file_name <> '') AND COALESCE(fc.feed_count, 0) > 0 THEN 'both'
                WHEN (s.file_name IS NOT NULL AND s.file_name <> '') THEN 'video'
                WHEN COALESCE(fc.feed_count, 0) > 0 THEN 'image'
                ELSE 'video'
              END AS content_type,
              lf.latest_feed_id AS image_feed_id,
              s.thumbnail,
              s.title,
              NULL::integer AS feed_id,
              s.created_at AS created_at,
              COALESCE(fc.feed_count, 0) AS popularity_score
            FROM fp_300 s
            JOIN fp_310 loc ON loc.place_id = s.place_id
            LEFT JOIN feed_counts fc ON fc.place_id = s.place_id
            LEFT JOIN latest_feeds lf ON lf.place_id = s.place_id
            %s
            """.formatted(distanceExpr, where);
    }

    private String buildVideoQuery(
        String keyword,
        String category,
        List<String> tags,
        Double lat,
        Double lng,
        Integer radiusM,
        String distanceExpr,
        MapSqlParameterSource params
    ) {
        StringBuilder where = new StringBuilder("WHERE s.use_yn = 'Y' AND s.open_yn = 'Y' AND s.deleted_at IS NULL");
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (s.store_name ILIKE :kw OR s.address ILIKE :kw OR s.title ILIKE :kw)");
        }
        if (category != null && !category.isBlank()) {
            where.append(" AND EXISTS (SELECT 1 FROM unnest(loc.types) t WHERE t ILIKE :category)");
        }
        where.append(buildTagFilter(tags, "store_id", "s.store_id", params, "tag_video_"));
        where.append(buildRadiusFilter(lat, lng, radiusM, distanceExpr));

        return """
            SELECT
              'video' AS type,
              s.place_id,
              s.store_id,
              s.store_name,
              s.address,
              loc.latitude AS lat,
              loc.longitude AS lng,
              %s AS distance_m,
              NULL::integer AS feed_count,
              NULL::varchar AS content_type,
              NULL::integer AS image_feed_id,
              s.thumbnail,
              s.title,
              NULL::integer AS feed_id,
              s.created_at AS created_at,
              0::int AS popularity_score
            FROM fp_300 s
            JOIN fp_310 loc ON loc.place_id = s.place_id
            %s
            """.formatted(distanceExpr, where);
    }

    private String buildImageQuery(
        String keyword,
        String category,
        List<String> tags,
        Double lat,
        Double lng,
        Integer radiusM,
        String distanceExpr,
        MapSqlParameterSource params
    ) {
        StringBuilder where = new StringBuilder("WHERE f.use_yn = 'Y'");
        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (f.store_name ILIKE :kw OR f.feed_title ILIKE :kw OR loc.formatted_address ILIKE :kw OR f.location ILIKE :kw)");
        }
        if (category != null && !category.isBlank()) {
            where.append(" AND EXISTS (SELECT 1 FROM unnest(loc.types) t WHERE t ILIKE :category)");
        }
        where.append(buildTagFilter(tags, "feed_id", "f.feed_no", params, "tag_image_"));
        where.append(buildRadiusFilter(lat, lng, radiusM, distanceExpr));

        return """
            SELECT
              'image' AS type,
              f.place_id,
              NULL::integer AS store_id,
              f.store_name,
              COALESCE(loc.formatted_address, f.location) AS address,
              loc.latitude AS lat,
              loc.longitude AS lng,
              %s AS distance_m,
              NULL::integer AS feed_count,
              NULL::varchar AS content_type,
              NULL::integer AS image_feed_id,
              f.thumbnail,
              f.feed_title AS title,
              f.feed_no AS feed_id,
              f.created_at AS created_at,
              0::int AS popularity_score
            FROM fp_400 f
            LEFT JOIN fp_310 loc ON loc.place_id = f.place_id
            %s
            """.formatted(distanceExpr, where);
    }

    private String buildTagFilter(
        List<String> tags,
        String tagOwnerColumn,
        String ownerColumnRef,
        MapSqlParameterSource params,
        String paramPrefix
    ) {
        if (tags == null || tags.isEmpty()) {
            return "";
        }
        List<String> cleanTags = tags.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(tag -> !tag.isBlank())
            .toList();
        if (cleanTags.isEmpty()) {
            return "";
        }
        List<String> conditions = new ArrayList<>();
        int index = 0;
        for (String tag : cleanTags) {
            String param = paramPrefix + index++;
            params.addValue(param, "%" + tag + "%", Types.VARCHAR);
            conditions.add("tag ILIKE :" + param);
        }
        String joined = String.join(" OR ", conditions);
        return " AND EXISTS (SELECT 1 FROM fp_350 t, regexp_split_to_table(t.tags, ',') AS tag"
            + " WHERE t." + tagOwnerColumn + " = " + ownerColumnRef
            + " AND (" + joined + "))";
    }

    private String buildRadiusFilter(Double lat, Double lng, Integer radiusM, String distanceExpr) {
        if (radiusM == null || lat == null || lng == null) {
            return "";
        }
        return """
            AND loc.latitude BETWEEN (:lat - (:radius_m / 111000.0)) AND (:lat + (:radius_m / 111000.0))
            AND loc.longitude BETWEEN
                (:lng - (:radius_m / (111000.0 * cos(radians(:lat)))))
                AND
                (:lng + (:radius_m / (111000.0 * cos(radians(:lat)))))
            AND (%s) <= :radius_m
            """.formatted(distanceExpr);
    }

    private String buildDistanceExpr(Double lat, Double lng) {
        if (lat == null || lng == null) {
            return "NULL::double precision";
        }
        return """
            6371000 * acos(
              LEAST(1.0, GREATEST(-1.0,
                cos(radians(:lat)) * cos(radians(loc.latitude)) *
                cos(radians(loc.longitude) - radians(:lng)) +
                sin(radians(:lat)) * sin(radians(loc.latitude))
              ))
            )
            """;
    }

    private String orderByClause(SearchSort sort) {
        SearchSort safeSort = (sort == null) ? SearchSort.RECENT : sort;
        return switch (safeSort) {
            case POPULAR -> "ORDER BY popularity_score DESC NULLS LAST, created_at DESC NULLS LAST";
            case DISTANCE -> "ORDER BY distance_m ASC NULLS LAST, created_at DESC NULLS LAST";
            case RECENT -> "ORDER BY created_at DESC NULLS LAST";
        };
    }

    private OffsetDateTime toOffset(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime.atOffset(ZoneOffset.UTC);
        }
        if (value instanceof LocalDate localDate) {
            return localDate.atStartOfDay().atOffset(ZoneOffset.UTC);
        }
        return null;
    }
}
