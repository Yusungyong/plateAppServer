package com.plateapp.plate_main.map.repository;

import com.plateapp.plate_main.map.dto.NearbyStoreMarkerDto;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.sql.Types;
import com.plateapp.plate_main.map.dto.MapSearchSuggestionDto;

@Repository
@RequiredArgsConstructor
public class MapNearbyRepository {

  private final NamedParameterJdbcTemplate jdbc;

  private static final String SQL_NEARBY_MARKERS = """
      WITH candidates AS (
        SELECT
          s.store_id,
          s.place_id,
          s.store_name,
          s.address,
          s.thumbnail,
          s.file_name AS video_file_name,
          s.username AS store_username,
          loc.latitude  AS lat,
          loc.longitude AS lng,
          COALESCE(:category, CASE
            WHEN EXISTS (SELECT 1 FROM unnest(loc.types) t WHERE lower(t) IN ('korean_restaurant', 'korean')) THEN 'KOREAN'
            WHEN EXISTS (SELECT 1 FROM unnest(loc.types) t WHERE lower(t) IN ('japanese_restaurant', 'japanese')) THEN 'JAPANESE'
            WHEN EXISTS (SELECT 1 FROM unnest(loc.types) t WHERE lower(t) IN ('chinese_restaurant', 'chinese')) THEN 'CHINESE'
            WHEN EXISTS (SELECT 1 FROM unnest(loc.types) t WHERE lower(t) IN ('cafe', 'coffee_shop')) THEN 'CAFE'
            WHEN EXISTS (SELECT 1 FROM unnest(loc.types) t WHERE lower(t) IN ('dessert_restaurant', 'bakery', 'ice_cream_shop', 'dessert')) THEN 'DESSERT'
            ELSE NULL
          END) AS category,
          (
            6371000 * acos(
              LEAST(1.0, GREATEST(-1.0,
                cos(radians(:lat)) * cos(radians(loc.latitude)) *
                cos(radians(loc.longitude) - radians(:lng)) +
                sin(radians(:lat)) * sin(radians(loc.latitude))
              ))
            )
          ) AS distance_m
        FROM fp_310 loc
        JOIN fp_300 s
          ON s.place_id = loc.place_id
        WHERE
          -- bbox guard
          loc.latitude BETWEEN (:lat - (:radius_m / 111000.0)) AND (:lat + (:radius_m / 111000.0))
          AND loc.longitude BETWEEN
              (:lng - (:radius_m / (111000.0 * cos(radians(:lat)))))
              AND
              (:lng + (:radius_m / (111000.0 * cos(radians(:lat)))))
          AND loc.use_yn = 'Y'
          AND loc.deleted_at IS NULL
          AND s.use_yn = 'Y'
          AND s.open_yn = 'Y'
          AND s.deleted_at IS NULL
          AND (:excluded_count = 0 OR s.username NOT IN (:excluded_usernames))
          AND (:group_place_id IS NULL OR s.place_id = :group_place_id)
          AND (:group_store_name IS NULL OR (s.place_id IS NULL AND s.store_name = :group_store_name))
          AND (:category IS NULL OR EXISTS (SELECT 1 FROM unnest(loc.types) t WHERE lower(t) IN (:category_types)))
      ),
      feed_counts AS (
        SELECT
          place_id,
          COUNT(*)::int AS feed_count
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
      )
      SELECT
        c.store_id,
        c.place_id,
        c.store_name,
        c.address,
        c.thumbnail,
        c.lat,
        c.lng,
        c.category,
        c.distance_m,
        COALESCE(f.feed_count, 0) AS feed_count,
        (c.video_file_name IS NOT NULL AND c.video_file_name <> '') AS has_video,
        lf.latest_feed_id
      FROM candidates c
      LEFT JOIN feed_counts f
        ON f.place_id = c.place_id
      LEFT JOIN latest_feeds lf
        ON lf.place_id = c.place_id
      WHERE c.distance_m <= :radius_m
      ORDER BY c.distance_m ASC
      LIMIT :limit
      """;

  public List<NearbyStoreMarkerDto> findNearby(
      double lat,
      double lng,
      int radiusM,
      int limit,
      List<String> excludedUsernames,
      String groupPlaceId,
      String groupStoreName,
      String category,
      List<String> categoryTypes
  ) {
    int excludedCount = (excludedUsernames == null) ? 0 : excludedUsernames.size();
    List<String> excluded = (excludedUsernames == null) ? List.of() : excludedUsernames;
    if (excludedCount == 0) {
      excluded = List.of("__none__");
    }
    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("lat", lat)
        .addValue("lng", lng)
        .addValue("radius_m", radiusM)
        .addValue("limit", limit)
        .addValue("excluded_usernames", excluded)
        .addValue("excluded_count", excludedCount)
        .addValue("group_place_id", groupPlaceId, Types.VARCHAR)
        .addValue("group_store_name", groupStoreName, Types.VARCHAR)
        .addValue("category", category, Types.VARCHAR)
        .addValue("category_types", categoryTypes);

    return jdbc.query(SQL_NEARBY_MARKERS, params, (rs, rowNum) -> {
      int feedCount = rs.getInt("feed_count");
      boolean hasVideo = rs.getBoolean("has_video");
      String contentType = determineContentType(hasVideo, feedCount);
      Integer representativeFeedId = rs.getObject("latest_feed_id", Integer.class);

      return new NearbyStoreMarkerDto(
          rs.getInt("store_id"),
          rs.getString("place_id"),
          rs.getString("store_name"),
          rs.getString("address"),
          rs.getString("thumbnail"),
          buildGroupId(rs.getString("place_id"), rs.getString("store_name")),
          rs.getObject("lat", Double.class),
          rs.getObject("lng", Double.class),
          (int) Math.round(rs.getDouble("distance_m")),
          feedCount,
          contentType,
          representativeFeedId,
          rs.getString("category")
      );
    });
  }

  private String buildGroupId(String placeId, String storeName) {
    if (placeId != null && !placeId.isBlank()) {
      return "place:" + placeId;
    }
    if (storeName != null && !storeName.isBlank()) {
      return "store:" + storeName;
    }
    return null;
  }

  private String determineContentType(boolean hasVideo, int feedCount) {
    if (hasVideo && feedCount > 0) {
      return "both";
    }
    if (hasVideo) {
      return "video";
    }
    if (feedCount > 0) {
      return "image";
    }
    return "video"; // default: map markers are video-store based
  }

  public List<NearbyStoreMarkerDto> searchStores(String keyword, Double lat, Double lng, int limit, List<String> excludedUsernames) {
    String base = """
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
        )
        SELECT
          s.store_id,
          s.place_id,
          s.store_name,
          s.address,
          s.thumbnail,
          loc.latitude  AS lat,
          loc.longitude AS lng,
          CASE
            WHEN EXISTS (SELECT 1 FROM unnest(loc.types) t WHERE lower(t) IN ('korean_restaurant', 'korean')) THEN 'KOREAN'
            WHEN EXISTS (SELECT 1 FROM unnest(loc.types) t WHERE lower(t) IN ('japanese_restaurant', 'japanese')) THEN 'JAPANESE'
            WHEN EXISTS (SELECT 1 FROM unnest(loc.types) t WHERE lower(t) IN ('chinese_restaurant', 'chinese')) THEN 'CHINESE'
            WHEN EXISTS (SELECT 1 FROM unnest(loc.types) t WHERE lower(t) IN ('cafe', 'coffee_shop')) THEN 'CAFE'
            WHEN EXISTS (SELECT 1 FROM unnest(loc.types) t WHERE lower(t) IN ('dessert_restaurant', 'bakery', 'ice_cream_shop', 'dessert')) THEN 'DESSERT'
            ELSE NULL
          END AS category,
          %s AS distance_m,
          COALESCE(fc.feed_count, 0) AS feed_count,
          (s.file_name IS NOT NULL AND s.file_name <> '') AS has_video,
          lf.latest_feed_id
        FROM fp_300 s
        JOIN fp_310 loc ON loc.place_id = s.place_id
        LEFT JOIN feed_counts fc ON fc.place_id = s.place_id
        LEFT JOIN latest_feeds lf ON lf.place_id = s.place_id
        WHERE s.use_yn = 'Y'
          AND s.open_yn = 'Y'
          AND s.deleted_at IS NULL
          AND (:excluded_count = 0 OR s.username NOT IN (:excluded_usernames))
          AND (
            s.store_name ILIKE :kw
            OR s.address ILIKE :kw
          )
        %s
        LIMIT :limit
        """;

    int excludedCount = (excludedUsernames == null) ? 0 : excludedUsernames.size();
    List<String> excluded = (excludedUsernames == null) ? List.of() : excludedUsernames;
    if (excludedCount == 0) {
      excluded = List.of("__none__");
    }
    MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("kw", "%" + keyword + "%")
            .addValue("limit", limit)
            .addValue("excluded_usernames", excluded)
            .addValue("excluded_count", excludedCount);

    String distanceExpr = "NULL";
    String orderBy = "ORDER BY s.created_at DESC NULLS LAST, s.store_id DESC";

    if (lat != null && lng != null) {
      params.addValue("lat", lat).addValue("lng", lng);
      distanceExpr = """
        6371000 * acos(
          LEAST(1.0, GREATEST(-1.0,
            cos(radians(:lat)) * cos(radians(loc.latitude)) *
            cos(radians(loc.longitude) - radians(:lng)) +
            sin(radians(:lat)) * sin(radians(loc.latitude))
          ))
        )
        """;
      orderBy = "ORDER BY distance_m ASC NULLS LAST";
    }

    String sql = String.format(base, distanceExpr, orderBy);

    return jdbc.query(sql, params, (rs, rowNum) -> {
      int feedCount = rs.getInt("feed_count");
      boolean hasVideo = rs.getBoolean("has_video");
      String contentType = determineContentType(hasVideo, feedCount);
      Integer representativeFeedId = rs.getObject("latest_feed_id", Integer.class);

      Double dLat = rs.getObject("lat", Double.class);
      Double dLng = rs.getObject("lng", Double.class);
      Integer distance = null;
      Double distanceRaw = rs.getObject("distance_m", Double.class);
      if (distanceRaw != null) {
        distance = (int) Math.round(distanceRaw);
      }

      return new NearbyStoreMarkerDto(
              rs.getInt("store_id"),
              rs.getString("place_id"),
              rs.getString("store_name"),
              rs.getString("address"),
              rs.getString("thumbnail"),
              buildGroupId(rs.getString("place_id"), rs.getString("store_name")),
              dLat,
              dLng,
              distance,
              feedCount,
              contentType,
              representativeFeedId,
              rs.getString("category")
      );
    });
  }

  public List<MapSearchSuggestionDto> suggestStores(String keyword, int limit) {
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
}
