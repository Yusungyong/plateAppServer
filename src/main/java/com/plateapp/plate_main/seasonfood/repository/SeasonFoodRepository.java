package com.plateapp.plate_main.seasonfood.repository;

import java.sql.Types;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SeasonFoodRepository {

  private static final String FLAG_Y = "Y";
  private static final String REGION_ALL = "REG_ALL";

  private final NamedParameterJdbcTemplate jdbc;

  public boolean existsCategory(String categoryId) {
    String sql = """
        SELECT COUNT(1)
        FROM fp_331_food_category
        WHERE category_id = :category_id
          AND use_yn = 'Y'
        """;
    Integer count = jdbc.queryForObject(
        sql,
        new MapSqlParameterSource().addValue("category_id", categoryId, Types.VARCHAR),
        Integer.class
    );
    return count != null && count > 0;
  }

  public boolean existsRegion(String regionId) {
    String sql = """
        SELECT COUNT(1)
        FROM fp_332_region
        WHERE region_id = :region_id
          AND use_yn = 'Y'
        """;
    Integer count = jdbc.queryForObject(
        sql,
        new MapSqlParameterSource().addValue("region_id", regionId, Types.VARCHAR),
        Integer.class
    );
    return count != null && count > 0;
  }

  public List<SeasonFoodListRow> findSeasonFoods(
      int month,
      String categoryId,
      String regionId,
      int minScore,
      int limit,
      int offset
  ) {
    String sql = """
        WITH RECURSIVE category_filter AS (
          SELECT category_id
          FROM fp_331_food_category
          WHERE category_id = :category_id
            AND use_yn = 'Y'
          UNION ALL
          SELECT child.category_id
          FROM fp_331_food_category child
          JOIN category_filter parent
            ON child.parent_category_id = parent.category_id
          WHERE child.use_yn = 'Y'
        )
        SELECT
          i.ingredient_id,
          i.ingredient_nm,
          i.ingredient_alias,
          i.thumbnail_url,
          i.description AS ingredient_description,
          c.category_id,
          c.category_nm,
          rep.region_id AS representative_region_id,
          rep.region_nm AS representative_region_nm,
          w.season_id,
          w.display_text,
          w.season_type_cd,
          w.confidence_cd,
          w.start_month,
          w.end_month,
          w.peak_month_text,
          ms.month_no,
          ms.season_score,
          ms.is_peak_yn,
          ms.score_label_cd
        FROM fp_335_season_month_score ms
        JOIN fp_334_season_window w
          ON w.season_id = ms.season_id
        JOIN fp_333_ingredient i
          ON i.ingredient_id = w.ingredient_id
        JOIN fp_331_food_category c
          ON c.category_id = i.category_id
        LEFT JOIN fp_332_region rep
          ON rep.region_id = i.representative_region_id
        JOIN fp_332_region r
          ON r.region_id = w.region_id
        WHERE ms.month_no = :month
          AND ms.season_score >= :min_score
          AND i.use_yn = 'Y'
          AND w.use_yn = 'Y'
          AND c.use_yn = 'Y'
          AND r.use_yn = 'Y'
          AND (:region_id IS NULL OR w.region_id = :region_id)
          AND (:category_id IS NULL OR i.category_id IN (SELECT category_id FROM category_filter))
        ORDER BY ms.is_peak_yn DESC,
                 ms.season_score DESC,
                 i.sort_order ASC,
                 i.ingredient_nm ASC
        LIMIT :limit
        OFFSET :offset
        """;

    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("month", month)
        .addValue("category_id", categoryId, Types.VARCHAR)
        .addValue("region_id", regionId, Types.VARCHAR)
        .addValue("min_score", minScore)
        .addValue("limit", limit)
        .addValue("offset", offset);

    return jdbc.query(sql, params, (rs, rowNum) -> new SeasonFoodListRow(
        rs.getString("ingredient_id"),
        rs.getString("ingredient_nm"),
        rs.getString("ingredient_alias"),
        rs.getString("thumbnail_url"),
        rs.getString("ingredient_description"),
        rs.getString("category_id"),
        rs.getString("category_nm"),
        rs.getString("representative_region_id"),
        rs.getString("representative_region_nm"),
        rs.getString("season_id"),
        rs.getString("display_text"),
        rs.getString("season_type_cd"),
        rs.getString("confidence_cd"),
        rs.getInt("start_month"),
        rs.getInt("end_month"),
        rs.getString("peak_month_text"),
        rs.getInt("month_no"),
        rs.getInt("season_score"),
        FLAG_Y.equalsIgnoreCase(rs.getString("is_peak_yn")),
        rs.getString("score_label_cd")
    ));
  }

  public Optional<IngredientRow> findIngredient(String ingredientId) {
    String sql = """
        SELECT
          i.ingredient_id,
          i.ingredient_nm,
          i.ingredient_alias,
          i.description,
          i.storage_tip,
          i.thumbnail_url,
          c.category_id,
          c.category_nm,
          rep.region_id AS representative_region_id,
          rep.region_nm AS representative_region_nm
        FROM fp_333_ingredient i
        JOIN fp_331_food_category c
          ON c.category_id = i.category_id
        LEFT JOIN fp_332_region rep
          ON rep.region_id = i.representative_region_id
        WHERE i.ingredient_id = :ingredient_id
          AND i.use_yn = 'Y'
          AND c.use_yn = 'Y'
        """;
    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("ingredient_id", ingredientId, Types.VARCHAR);

    List<IngredientRow> rows = jdbc.query(sql, params, (rs, rowNum) -> new IngredientRow(
        rs.getString("ingredient_id"),
        rs.getString("ingredient_nm"),
        rs.getString("ingredient_alias"),
        rs.getString("description"),
        rs.getString("storage_tip"),
        rs.getString("thumbnail_url"),
        rs.getString("category_id"),
        rs.getString("category_nm"),
        rs.getString("representative_region_id"),
        rs.getString("representative_region_nm")
    ));
    return rows.stream().findFirst();
  }

  public List<SeasonWindowRow> findSeasonWindows(String ingredientId, String regionId) {
    List<String> regionIds = regionId == null || regionId.isBlank()
        ? List.of("__none__")
        : REGION_ALL.equals(regionId) ? List.of(regionId) : List.of(regionId, REGION_ALL);
    String sql = """
        SELECT
          w.season_id,
          w.ingredient_id,
          w.region_id,
          r.region_nm,
          w.season_type_cd,
          w.start_month,
          w.end_month,
          w.peak_month_text,
          w.display_text,
          w.confidence_cd,
          w.description,
          w.sort_order
        FROM fp_334_season_window w
        JOIN fp_332_region r
          ON r.region_id = w.region_id
        WHERE w.ingredient_id = :ingredient_id
          AND w.use_yn = 'Y'
          AND r.use_yn = 'Y'
          AND (:region_id IS NULL OR w.region_id IN (:region_ids))
        ORDER BY CASE
                   WHEN :region_id IS NOT NULL AND w.region_id = :region_id THEN 0
                   WHEN w.region_id = 'REG_ALL' THEN 1
                   ELSE 2
                 END,
                 w.sort_order ASC,
                 w.season_id ASC
        """;
    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("ingredient_id", ingredientId, Types.VARCHAR)
        .addValue("region_id", regionId, Types.VARCHAR)
        .addValue("region_ids", regionIds);

    return jdbc.query(sql, params, (rs, rowNum) -> new SeasonWindowRow(
        rs.getString("season_id"),
        rs.getString("ingredient_id"),
        rs.getString("region_id"),
        rs.getString("region_nm"),
        rs.getString("season_type_cd"),
        rs.getInt("start_month"),
        rs.getInt("end_month"),
        rs.getString("peak_month_text"),
        rs.getString("display_text"),
        rs.getString("confidence_cd"),
        rs.getString("description"),
        rs.getInt("sort_order")
    ));
  }

  public List<MonthScoreRow> findMonthScores(List<String> seasonIds) {
    if (seasonIds == null || seasonIds.isEmpty()) {
      return List.of();
    }
    String sql = """
        SELECT
          season_id,
          month_no,
          season_score,
          is_peak_yn,
          score_label_cd,
          description
        FROM fp_335_season_month_score
        WHERE season_id IN (:season_ids)
        ORDER BY season_id ASC, month_no ASC
        """;
    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("season_ids", seasonIds);

    return jdbc.query(sql, params, (rs, rowNum) -> new MonthScoreRow(
        rs.getString("season_id"),
        rs.getInt("month_no"),
        rs.getInt("season_score"),
        FLAG_Y.equalsIgnoreCase(rs.getString("is_peak_yn")),
        rs.getString("score_label_cd"),
        rs.getString("description")
    ));
  }

  public List<ReasonRow> findReasons(List<String> seasonIds) {
    if (seasonIds == null || seasonIds.isEmpty()) {
      return List.of();
    }
    String sql = """
        SELECT
          season_id,
          reason_cd,
          reason_title,
          description,
          sort_order
        FROM fp_336_season_reason
        WHERE season_id IN (:season_ids)
        ORDER BY season_id ASC, sort_order ASC, reason_id ASC
        """;
    return jdbc.query(
        sql,
        new MapSqlParameterSource().addValue("season_ids", seasonIds),
        (rs, rowNum) -> new ReasonRow(
            rs.getString("season_id"),
            rs.getString("reason_cd"),
            rs.getString("reason_title"),
            rs.getString("description"),
            rs.getInt("sort_order")
        )
    );
  }

  public List<SourceRow> findSources(List<String> seasonIds) {
    if (seasonIds == null || seasonIds.isEmpty()) {
      return List.of();
    }
    String sql = """
        SELECT
          season_id,
          source_type_cd,
          source_nm,
          source_url,
          checked_at,
          reliability_cd
        FROM fp_339_season_source
        WHERE season_id IN (:season_ids)
        ORDER BY season_id ASC, checked_at DESC NULLS LAST, source_id ASC
        """;
    return jdbc.query(
        sql,
        new MapSqlParameterSource().addValue("season_ids", seasonIds),
        (rs, rowNum) -> new SourceRow(
            rs.getString("season_id"),
            rs.getString("source_type_cd"),
            rs.getString("source_nm"),
            rs.getString("source_url"),
            rs.getObject("checked_at", LocalDate.class),
            rs.getString("reliability_cd")
        )
    );
  }

  public List<DishRow> findDishes(String ingredientId) {
    String sql = """
        SELECT
          d.dish_id,
          d.dish_nm,
          d.cooking_type_cd,
          d.thumbnail_url,
          id.recommend_months,
          id.is_represent_yn,
          id.description,
          id.sort_order
        FROM fp_338_ingredient_dish id
        JOIN fp_337_dish d
          ON d.dish_id = id.dish_id
        WHERE id.ingredient_id = :ingredient_id
          AND d.use_yn = 'Y'
        ORDER BY id.is_represent_yn DESC,
                 id.sort_order ASC,
                 d.dish_nm ASC
        """;
    MapSqlParameterSource params = new MapSqlParameterSource()
        .addValue("ingredient_id", ingredientId, Types.VARCHAR);

    return jdbc.query(sql, params, (rs, rowNum) -> new DishRow(
        rs.getString("dish_id"),
        rs.getString("dish_nm"),
        rs.getString("cooking_type_cd"),
        rs.getString("thumbnail_url"),
        rs.getString("recommend_months"),
        FLAG_Y.equalsIgnoreCase(rs.getString("is_represent_yn")),
        rs.getString("description"),
        rs.getInt("sort_order")
    ));
  }

  public List<CategoryRow> findCategories(String type) {
    String sql = """
        SELECT
          category_id,
          parent_category_id,
          category_nm,
          category_level,
          sort_order
        FROM fp_331_food_category
        WHERE category_type_cd = :type
          AND use_yn = 'Y'
        ORDER BY category_level ASC,
                 sort_order ASC,
                 category_nm ASC
        """;
    return jdbc.query(
        sql,
        new MapSqlParameterSource().addValue("type", type, Types.VARCHAR),
        (rs, rowNum) -> new CategoryRow(
            rs.getString("category_id"),
            rs.getString("parent_category_id"),
            rs.getString("category_nm"),
            rs.getInt("category_level"),
            rs.getInt("sort_order")
        )
    );
  }

  public List<RegionRow> findRegions(String type) {
    String sql = """
        SELECT
          region_id,
          parent_region_id,
          region_nm,
          region_type_cd,
          sort_order
        FROM fp_332_region
        WHERE use_yn = 'Y'
          AND (:type IS NULL OR region_type_cd = :type)
        ORDER BY CASE WHEN parent_region_id IS NULL THEN 0 ELSE 1 END,
                 sort_order ASC,
                 region_nm ASC
        """;
    return jdbc.query(
        sql,
        new MapSqlParameterSource().addValue("type", type, Types.VARCHAR),
        (rs, rowNum) -> new RegionRow(
            rs.getString("region_id"),
            rs.getString("parent_region_id"),
            rs.getString("region_nm"),
            rs.getString("region_type_cd"),
            rs.getInt("sort_order")
        )
    );
  }

  public record SeasonFoodListRow(
      String ingredientId,
      String ingredientName,
      String aliases,
      String thumbnailUrl,
      String description,
      String categoryId,
      String categoryName,
      String representativeRegionId,
      String representativeRegionName,
      String seasonId,
      String displayText,
      String seasonType,
      String confidence,
      int startMonth,
      int endMonth,
      String peakMonthText,
      int month,
      int seasonScore,
      boolean isPeak,
      String scoreLabel
  ) {}

  public record IngredientRow(
      String ingredientId,
      String ingredientName,
      String aliases,
      String description,
      String storageTip,
      String thumbnailUrl,
      String categoryId,
      String categoryName,
      String representativeRegionId,
      String representativeRegionName
  ) {}

  public record SeasonWindowRow(
      String seasonId,
      String ingredientId,
      String regionId,
      String regionName,
      String seasonType,
      int startMonth,
      int endMonth,
      String peakMonthText,
      String displayText,
      String confidence,
      String description,
      int sortOrder
  ) {}

  public record MonthScoreRow(
      String seasonId,
      int month,
      int seasonScore,
      boolean isPeak,
      String scoreLabel,
      String description
  ) {}

  public record ReasonRow(
      String seasonId,
      String reasonCode,
      String title,
      String description,
      int sortOrder
  ) {}

  public record SourceRow(
      String seasonId,
      String sourceType,
      String name,
      String url,
      LocalDate checkedAt,
      String reliability
  ) {}

  public record DishRow(
      String dishId,
      String name,
      String cookingType,
      String thumbnailUrl,
      String recommendMonths,
      boolean isRepresentative,
      String description,
      int sortOrder
  ) {}

  public record CategoryRow(
      String categoryId,
      String parentCategoryId,
      String name,
      int level,
      int sortOrder
  ) {}

  public record RegionRow(
      String regionId,
      String parentRegionId,
      String name,
      String type,
      int sortOrder
  ) {}
}
