package com.plateapp.plate_main.seasonfood.repository;

import java.math.BigDecimal;
import java.sql.Types;
import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SeasonStoreMatchRepository {

  private static final String FLAG_Y = "Y";

  private final NamedParameterJdbcTemplate jdbc;

  public boolean matchTablesReady() {
    String sql = """
        SELECT
          to_regclass('fp_340_season_match_keyword') IS NOT NULL
          AND to_regclass('fp_341_season_store_match') IS NOT NULL
          AND to_regclass('fp_342_season_match_evidence') IS NOT NULL
          AND to_regclass('fp_343_season_store_match_override') IS NOT NULL
        """;
    try {
      Boolean ready = jdbc.queryForObject(sql, new MapSqlParameterSource(), Boolean.class);
      return Boolean.TRUE.equals(ready);
    } catch (DataAccessException e) {
      return false;
    }
  }

  public IngredientSeasonRow findIngredientSeason(String ingredientId, int month, String regionId) {
    String sql = """
        SELECT
          i.ingredient_id,
          i.ingredient_nm,
          ms.season_score,
          ms.is_peak_yn
        FROM fp_333_ingredient i
        LEFT JOIN fp_334_season_window w
          ON w.ingredient_id = i.ingredient_id
         AND w.use_yn = 'Y'
         AND (:region_id IS NULL OR w.region_id IN (:region_ids))
        LEFT JOIN fp_335_season_month_score ms
          ON ms.season_id = w.season_id
         AND ms.month_no = :month
        WHERE i.ingredient_id = :ingredient_id
          AND i.use_yn = 'Y'
        ORDER BY CASE
                   WHEN :region_id IS NOT NULL AND w.region_id = :region_id THEN 0
                   WHEN w.region_id = 'REG_ALL' THEN 1
                   ELSE 2
                 END,
                 ms.season_score DESC NULLS LAST
        LIMIT 1
        """;
    MapSqlParameterSource params = baseIngredientParams(ingredientId, month, regionId);
    List<IngredientSeasonRow> rows = jdbc.query(sql, params, (rs, rowNum) -> new IngredientSeasonRow(
        rs.getString("ingredient_id"),
        rs.getString("ingredient_nm"),
        rs.getObject("season_score", Integer.class),
        FLAG_Y.equalsIgnoreCase(rs.getString("is_peak_yn"))
    ));
    return rows.isEmpty() ? null : rows.get(0);
  }

  public List<SeasonStoreMatchRow> findIngredientStores(
      String ingredientId,
      int month,
      Double lat,
      Double lng,
      Integer radiusM,
      String regionId,
      int limit,
      int offset
  ) {
    String sql = """
        WITH base AS (
          SELECT
            m.match_id,
            m.ingredient_id,
            i.ingredient_nm,
            m.store_id,
            m.restaurant_id,
            m.place_id,
            m.store_name,
            m.address,
            m.representative_menu_name,
            m.representative_image_url,
            m.match_score,
            COALESCE(ms.season_score, m.season_score) AS season_score,
            COALESCE(ms.is_peak_yn, 'N') AS is_peak_yn,
            m.evidence_count,
            m.match_status_cd,
            m.match_source_cd,
            m.matched_keywords,
            m.reason_text,
            CASE
              WHEN :lat IS NULL OR :lng IS NULL OR loc.latitude IS NULL OR loc.longitude IS NULL THEN NULL
              ELSE 6371000 * acos(
                LEAST(1.0, GREATEST(-1.0,
                  cos(radians(:lat)) * cos(radians(loc.latitude)) *
                  cos(radians(loc.longitude) - radians(:lng)) +
                  sin(radians(:lat)) * sin(radians(loc.latitude))
                ))
              )
            END AS distance_m
          FROM fp_341_season_store_match m
          JOIN fp_333_ingredient i
            ON i.ingredient_id = m.ingredient_id
          LEFT JOIN fp_334_season_window w
            ON w.season_id = m.season_id
          LEFT JOIN fp_335_season_month_score ms
            ON ms.season_id = m.season_id
           AND ms.month_no = :month
          LEFT JOIN fp_310 loc
            ON loc.place_id = m.place_id
           AND loc.use_yn = 'Y'
           AND loc.deleted_at IS NULL
          WHERE m.ingredient_id = :ingredient_id
            AND m.use_yn = 'Y'
            AND m.match_status_cd <> 'REJECTED'
            AND (m.expires_at IS NULL OR m.expires_at > NOW())
            AND i.use_yn = 'Y'
            AND (:region_id IS NULL OR w.region_id = :region_id OR w.region_id IS NULL)
        )
        SELECT *
        FROM base
        WHERE (:radius_m IS NULL OR distance_m <= :radius_m)
        ORDER BY
          distance_m ASC NULLS LAST,
          CASE WHEN match_status_cd = 'CONFIRMED' THEN 0 ELSE 1 END,
          season_score DESC NULLS LAST,
          match_score DESC,
          store_name ASC NULLS LAST
        LIMIT :limit
        OFFSET :offset
        """;
    MapSqlParameterSource params = storeQueryParams(ingredientId, month, lat, lng, radiusM, regionId, limit, offset);
    return jdbc.query(sql, params, (rs, rowNum) -> mapStoreMatch(rs));
  }

  public List<SeasonStoreMatchRow> findNearbyStores(
      int month,
      double lat,
      double lng,
      int radiusM,
      String categoryId,
      int limit
  ) {
    String sql = """
        WITH base AS (
          SELECT
            m.match_id,
            m.ingredient_id,
            i.ingredient_nm,
            m.store_id,
            m.restaurant_id,
            m.place_id,
            m.store_name,
            m.address,
            m.representative_menu_name,
            m.representative_image_url,
            m.match_score,
            COALESCE(ms.season_score, m.season_score) AS season_score,
            COALESCE(ms.is_peak_yn, 'N') AS is_peak_yn,
            m.evidence_count,
            m.match_status_cd,
            m.match_source_cd,
            m.matched_keywords,
            m.reason_text,
            6371000 * acos(
              LEAST(1.0, GREATEST(-1.0,
                cos(radians(:lat)) * cos(radians(loc.latitude)) *
                cos(radians(loc.longitude) - radians(:lng)) +
                sin(radians(:lat)) * sin(radians(loc.latitude))
              ))
            ) AS distance_m
          FROM fp_341_season_store_match m
          JOIN fp_333_ingredient i
            ON i.ingredient_id = m.ingredient_id
          LEFT JOIN fp_335_season_month_score ms
            ON ms.season_id = m.season_id
           AND ms.month_no = :month
          JOIN fp_310 loc
            ON loc.place_id = m.place_id
           AND loc.use_yn = 'Y'
           AND loc.deleted_at IS NULL
           AND loc.latitude IS NOT NULL
           AND loc.longitude IS NOT NULL
          WHERE m.use_yn = 'Y'
            AND m.match_status_cd <> 'REJECTED'
            AND (m.expires_at IS NULL OR m.expires_at > NOW())
            AND i.use_yn = 'Y'
            AND (:category_id IS NULL OR i.category_id = :category_id)
        )
        SELECT *
        FROM base
        WHERE distance_m <= :radius_m
        ORDER BY
          distance_m ASC,
          season_score DESC NULLS LAST,
          CASE WHEN match_status_cd = 'CONFIRMED' THEN 0 ELSE 1 END,
          match_score DESC
        LIMIT :limit
        """;
    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("month", month)
        .addValue("lat", lat)
        .addValue("lng", lng)
        .addValue("radius_m", radiusM)
        .addValue("category_id", categoryId, Types.VARCHAR)
        .addValue("limit", limit);
    return jdbc.query(sql, params, (rs, rowNum) -> mapStoreMatch(rs));
  }

  public List<SeasonStoreMatchRow> findStoreSeasonFoods(Integer storeId, int month) {
    String sql = """
        SELECT
          m.match_id,
          m.ingredient_id,
          i.ingredient_nm,
          m.store_id,
          m.restaurant_id,
          m.place_id,
          m.store_name,
          m.address,
          m.representative_menu_name,
          m.representative_image_url,
          m.match_score,
          COALESCE(ms.season_score, m.season_score) AS season_score,
          COALESCE(ms.is_peak_yn, 'N') AS is_peak_yn,
          m.evidence_count,
          m.match_status_cd,
          m.match_source_cd,
          m.matched_keywords,
          m.reason_text,
          NULL::double precision AS distance_m
        FROM fp_341_season_store_match m
        JOIN fp_333_ingredient i
          ON i.ingredient_id = m.ingredient_id
        LEFT JOIN fp_335_season_month_score ms
          ON ms.season_id = m.season_id
         AND ms.month_no = :month
        WHERE m.store_id = :store_id
          AND m.use_yn = 'Y'
          AND m.match_status_cd <> 'REJECTED'
          AND (m.expires_at IS NULL OR m.expires_at > NOW())
          AND i.use_yn = 'Y'
        ORDER BY
          season_score DESC NULLS LAST,
          CASE WHEN m.match_status_cd = 'CONFIRMED' THEN 0 ELSE 1 END,
          m.match_score DESC
        """;
    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("store_id", storeId)
        .addValue("month", month);
    return jdbc.query(sql, params, (rs, rowNum) -> mapStoreMatch(rs));
  }

  public List<SeasonStoreMatchRow> findHomeMatches(
      int month,
      Double lat,
      Double lng,
      int fetchLimit
  ) {
    String sql = """
        SELECT *
        FROM (
          SELECT
            m.match_id,
            m.ingredient_id,
            i.ingredient_nm,
            m.store_id,
            m.restaurant_id,
            m.place_id,
            m.store_name,
            m.address,
            m.representative_menu_name,
            m.representative_image_url,
            m.match_score,
            COALESCE(ms.season_score, m.season_score) AS season_score,
            COALESCE(ms.is_peak_yn, 'N') AS is_peak_yn,
            m.evidence_count,
            m.match_status_cd,
            m.match_source_cd,
            m.matched_keywords,
            m.reason_text,
            CASE
              WHEN :lat IS NULL OR :lng IS NULL OR loc.latitude IS NULL OR loc.longitude IS NULL THEN NULL
              ELSE 6371000 * acos(
                LEAST(1.0, GREATEST(-1.0,
                  cos(radians(:lat)) * cos(radians(loc.latitude)) *
                  cos(radians(loc.longitude) - radians(:lng)) +
                  sin(radians(:lat)) * sin(radians(loc.latitude))
                ))
              )
            END AS distance_m
          FROM fp_341_season_store_match m
          JOIN fp_333_ingredient i
            ON i.ingredient_id = m.ingredient_id
          LEFT JOIN fp_335_season_month_score ms
            ON ms.season_id = m.season_id
           AND ms.month_no = :month
          LEFT JOIN fp_310 loc
            ON loc.place_id = m.place_id
           AND loc.use_yn = 'Y'
           AND loc.deleted_at IS NULL
          WHERE m.use_yn = 'Y'
            AND m.match_status_cd <> 'REJECTED'
            AND (m.expires_at IS NULL OR m.expires_at > NOW())
            AND i.use_yn = 'Y'
        ) base
        ORDER BY
          season_score DESC NULLS LAST,
          distance_m ASC NULLS LAST,
          CASE WHEN match_status_cd = 'CONFIRMED' THEN 0 ELSE 1 END,
          match_score DESC
        LIMIT :limit
        """;
    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("month", month)
        .addValue("lat", lat, Types.DOUBLE)
        .addValue("lng", lng, Types.DOUBLE)
        .addValue("limit", fetchLimit);
    return jdbc.query(sql, params, (rs, rowNum) -> mapStoreMatch(rs));
  }

  private MapSqlParameterSource baseIngredientParams(String ingredientId, int month, String regionId) {
    List<String> regionIds = regionId == null || regionId.isBlank()
        ? List.of("__none__")
        : "REG_ALL".equals(regionId) ? List.of(regionId) : List.of(regionId, "REG_ALL");
    return new MapSqlParameterSource()
        .addValue("ingredient_id", ingredientId, Types.VARCHAR)
        .addValue("month", month)
        .addValue("region_id", regionId, Types.VARCHAR)
        .addValue("region_ids", regionIds);
  }

  private MapSqlParameterSource storeQueryParams(
      String ingredientId,
      int month,
      Double lat,
      Double lng,
      Integer radiusM,
      String regionId,
      int limit,
      int offset
  ) {
    return new MapSqlParameterSource()
        .addValue("ingredient_id", ingredientId, Types.VARCHAR)
        .addValue("month", month)
        .addValue("lat", lat, Types.DOUBLE)
        .addValue("lng", lng, Types.DOUBLE)
        .addValue("radius_m", radiusM, Types.INTEGER)
        .addValue("region_id", regionId, Types.VARCHAR)
        .addValue("limit", limit)
        .addValue("offset", offset);
  }

  private SeasonStoreMatchRow mapStoreMatch(java.sql.ResultSet rs) throws java.sql.SQLException {
    Double distanceRaw = rs.getObject("distance_m", Double.class);
    Integer distanceM = distanceRaw == null ? null : (int) Math.round(distanceRaw);
    return new SeasonStoreMatchRow(
        rs.getObject("match_id", Long.class),
        rs.getString("ingredient_id"),
        rs.getString("ingredient_nm"),
        rs.getObject("store_id", Integer.class),
        rs.getObject("restaurant_id", Long.class),
        rs.getString("place_id"),
        rs.getString("store_name"),
        rs.getString("address"),
        rs.getString("representative_menu_name"),
        rs.getString("representative_image_url"),
        rs.getObject("match_score", BigDecimal.class),
        rs.getObject("season_score", Integer.class),
        FLAG_Y.equalsIgnoreCase(rs.getString("is_peak_yn")),
        rs.getInt("evidence_count"),
        rs.getString("match_status_cd"),
        rs.getString("match_source_cd"),
        rs.getString("matched_keywords"),
        rs.getString("reason_text"),
        distanceM
    );
  }

  public record IngredientSeasonRow(
      String ingredientId,
      String ingredientName,
      Integer seasonScore,
      boolean isPeak
  ) {}

  public record SeasonStoreMatchRow(
      Long matchId,
      String ingredientId,
      String ingredientName,
      Integer storeId,
      Long restaurantId,
      String placeId,
      String storeName,
      String address,
      String representativeMenuName,
      String representativeImageUrl,
      BigDecimal matchScore,
      Integer seasonScore,
      boolean isPeak,
      int evidenceCount,
      String matchStatus,
      String matchSource,
      String matchedKeywords,
      String reasonText,
      Integer distanceM
  ) {}
}
