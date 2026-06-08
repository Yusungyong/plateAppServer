package com.plateapp.plate_main.seasonfood.repository;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AdminSeasonMatchRepository {

  private final NamedParameterJdbcTemplate jdbc;

  public boolean existsActiveKeyword(String ingredientId, String keyword, String keywordType, Long excludeKeywordId) {
    String sql = """
        SELECT COUNT(1)
        FROM fp_340_season_match_keyword
        WHERE ingredient_id = :ingredient_id
          AND lower(keyword) = lower(:keyword)
          AND keyword_type_cd = :keyword_type
          AND use_yn = 'Y'
          AND (:exclude_keyword_id IS NULL OR keyword_id <> :exclude_keyword_id)
        """;
    Integer count = jdbc.queryForObject(
        sql,
        new MapSqlParameterSource()
            .addValue("ingredient_id", ingredientId, Types.VARCHAR)
            .addValue("keyword", keyword, Types.VARCHAR)
            .addValue("keyword_type", keywordType, Types.VARCHAR)
            .addValue("exclude_keyword_id", excludeKeywordId, Types.BIGINT),
        Integer.class
    );
    return count != null && count > 0;
  }

  public List<KeywordRow> findKeywords(
      String ingredientId,
      String keywordType,
      String keyword,
      int limit,
      int offset
  ) {
    String sql = """
        SELECT
          k.keyword_id,
          k.ingredient_id,
          i.ingredient_nm,
          k.keyword,
          k.keyword_type_cd,
          k.match_weight,
          k.exact_match_yn,
          k.description,
          k.use_yn,
          k.created_at,
          k.updated_at
        FROM fp_340_season_match_keyword k
        JOIN fp_333_ingredient i
          ON i.ingredient_id = k.ingredient_id
        WHERE (:ingredient_id IS NULL OR k.ingredient_id = :ingredient_id)
          AND (:keyword_type IS NULL OR k.keyword_type_cd = :keyword_type)
          AND (:keyword IS NULL OR k.keyword ILIKE :keyword_like)
        ORDER BY k.use_yn DESC,
                 i.sort_order ASC,
                 k.keyword_type_cd ASC,
                 k.keyword ASC
        LIMIT :limit
        OFFSET :offset
        """;
    MapSqlParameterSource params = keywordSearchParams(ingredientId, keywordType, keyword, limit, offset);
    return jdbc.query(sql, params, (rs, rowNum) -> new KeywordRow(
        rs.getObject("keyword_id", Long.class),
        rs.getString("ingredient_id"),
        rs.getString("ingredient_nm"),
        rs.getString("keyword"),
        rs.getString("keyword_type_cd"),
        rs.getObject("match_weight", BigDecimal.class),
        "Y".equalsIgnoreCase(rs.getString("exact_match_yn")),
        rs.getString("description"),
        rs.getString("use_yn"),
        rs.getObject("created_at", OffsetDateTime.class),
        rs.getObject("updated_at", OffsetDateTime.class)
    ));
  }

  public List<KeywordRow> findKeyword(Long keywordId) {
    String sql = """
        SELECT
          k.keyword_id,
          k.ingredient_id,
          i.ingredient_nm,
          k.keyword,
          k.keyword_type_cd,
          k.match_weight,
          k.exact_match_yn,
          k.description,
          k.use_yn,
          k.created_at,
          k.updated_at
        FROM fp_340_season_match_keyword k
        JOIN fp_333_ingredient i
          ON i.ingredient_id = k.ingredient_id
        WHERE k.keyword_id = :keyword_id
        """;
    return jdbc.query(
        sql,
        new MapSqlParameterSource().addValue("keyword_id", keywordId),
        (rs, rowNum) -> new KeywordRow(
            rs.getObject("keyword_id", Long.class),
            rs.getString("ingredient_id"),
            rs.getString("ingredient_nm"),
            rs.getString("keyword"),
            rs.getString("keyword_type_cd"),
            rs.getObject("match_weight", BigDecimal.class),
            "Y".equalsIgnoreCase(rs.getString("exact_match_yn")),
            rs.getString("description"),
            rs.getString("use_yn"),
            rs.getObject("created_at", OffsetDateTime.class),
            rs.getObject("updated_at", OffsetDateTime.class)
        )
    );
  }

  public Long insertKeyword(
      String ingredientId,
      String keyword,
      String keywordType,
      BigDecimal matchWeight,
      boolean exactMatch,
      String description
  ) {
    String sql = """
        INSERT INTO fp_340_season_match_keyword
          (ingredient_id, keyword, keyword_type_cd, match_weight, exact_match_yn, description)
        VALUES
          (:ingredient_id, :keyword, :keyword_type, :match_weight, :exact_match_yn, :description)
        RETURNING keyword_id
        """;
    return jdbc.queryForObject(
        sql,
        keywordWriteParams(ingredientId, keyword, keywordType, matchWeight, exactMatch, description),
        Long.class
    );
  }

  public int updateKeyword(
      Long keywordId,
      String ingredientId,
      String keyword,
      String keywordType,
      BigDecimal matchWeight,
      boolean exactMatch,
      String description
  ) {
    String sql = """
        UPDATE fp_340_season_match_keyword
        SET ingredient_id = :ingredient_id,
            keyword = :keyword,
            keyword_type_cd = :keyword_type,
            match_weight = :match_weight,
            exact_match_yn = :exact_match_yn,
            description = :description,
            updated_at = NOW()
        WHERE keyword_id = :keyword_id
        """;
    MapSqlParameterSource params = keywordWriteParams(
        ingredientId,
        keyword,
        keywordType,
        matchWeight,
        exactMatch,
        description
    ).addValue("keyword_id", keywordId);
    return jdbc.update(sql, params);
  }

  public int disableKeyword(Long keywordId) {
    String sql = """
        UPDATE fp_340_season_match_keyword
        SET use_yn = 'N',
            updated_at = NOW()
        WHERE keyword_id = :keyword_id
        """;
    return jdbc.update(sql, new MapSqlParameterSource().addValue("keyword_id", keywordId));
  }

  public List<MatchAdminRow> findMatches(
      String ingredientId,
      String status,
      String source,
      String keyword,
      int limit,
      int offset
  ) {
    String sql = """
        SELECT
          m.match_id,
          m.ingredient_id,
          i.ingredient_nm,
          m.store_id,
          m.restaurant_id,
          m.place_id,
          m.store_name,
          m.representative_menu_name,
          m.match_score,
          m.season_score,
          m.evidence_count,
          m.match_status_cd,
          m.match_source_cd,
          m.matched_keywords,
          m.generated_at,
          m.expires_at
        FROM fp_341_season_store_match m
        JOIN fp_333_ingredient i
          ON i.ingredient_id = m.ingredient_id
        WHERE m.use_yn = 'Y'
          AND (:ingredient_id IS NULL OR m.ingredient_id = :ingredient_id)
          AND (:status IS NULL OR m.match_status_cd = :status)
          AND (:source IS NULL OR m.match_source_cd = :source)
          AND (
            :keyword IS NULL
            OR m.store_name ILIKE :keyword_like
            OR m.representative_menu_name ILIKE :keyword_like
            OR m.matched_keywords ILIKE :keyword_like
          )
        ORDER BY
          CASE WHEN m.match_status_cd = 'CONFIRMED' THEN 0 WHEN m.match_status_cd = 'AUTO' THEN 1 ELSE 2 END,
          m.match_score DESC,
          m.generated_at DESC
        LIMIT :limit
        OFFSET :offset
        """;
    MapSqlParameterSource params = matchSearchParams(ingredientId, status, source, keyword, limit, offset);
    return jdbc.query(sql, params, (rs, rowNum) -> mapMatchAdminRow(rs));
  }

  public List<MatchAdminRow> findMatch(Long matchId) {
    String sql = """
        SELECT
          m.match_id,
          m.ingredient_id,
          i.ingredient_nm,
          m.store_id,
          m.restaurant_id,
          m.place_id,
          m.store_name,
          m.representative_menu_name,
          m.match_score,
          m.season_score,
          m.evidence_count,
          m.match_status_cd,
          m.match_source_cd,
          m.matched_keywords,
          m.generated_at,
          m.expires_at
        FROM fp_341_season_store_match m
        JOIN fp_333_ingredient i
          ON i.ingredient_id = m.ingredient_id
        WHERE m.match_id = :match_id
          AND m.use_yn = 'Y'
        """;
    return jdbc.query(
        sql,
        new MapSqlParameterSource().addValue("match_id", matchId),
        (rs, rowNum) -> mapMatchAdminRow(rs)
    );
  }

  public List<EvidenceRow> findEvidence(Long matchId) {
    String sql = """
        SELECT
          evidence_id,
          target_type_cd,
          target_id,
          matched_field,
          matched_keyword,
          matched_text,
          field_weight,
          keyword_weight,
          score,
          created_at
        FROM fp_342_season_match_evidence
        WHERE match_id = :match_id
        ORDER BY score DESC,
                 evidence_id ASC
        """;
    return jdbc.query(
        sql,
        new MapSqlParameterSource().addValue("match_id", matchId),
        (rs, rowNum) -> new EvidenceRow(
            rs.getObject("evidence_id", Long.class),
            rs.getString("target_type_cd"),
            rs.getString("target_id"),
            rs.getString("matched_field"),
            rs.getString("matched_keyword"),
            rs.getString("matched_text"),
            rs.getObject("field_weight", BigDecimal.class),
            rs.getObject("keyword_weight", BigDecimal.class),
            rs.getObject("score", BigDecimal.class),
            rs.getObject("created_at", OffsetDateTime.class)
        )
    );
  }

  public int confirmMatch(Long matchId, String menuName, String imageUrl) {
    String sql = """
        UPDATE fp_341_season_store_match
        SET match_status_cd = 'CONFIRMED',
            representative_menu_name = COALESCE(:menu_name, representative_menu_name),
            representative_image_url = COALESCE(:image_url, representative_image_url),
            updated_at = NOW()
        WHERE match_id = :match_id
          AND use_yn = 'Y'
        """;
    return jdbc.update(
        sql,
        new MapSqlParameterSource()
            .addValue("match_id", matchId)
            .addValue("menu_name", blankToNull(menuName), Types.VARCHAR)
            .addValue("image_url", blankToNull(imageUrl), Types.VARCHAR)
    );
  }

  public int rejectMatch(Long matchId) {
    String sql = """
        UPDATE fp_341_season_store_match
        SET match_status_cd = 'REJECTED',
            updated_at = NOW()
        WHERE match_id = :match_id
          AND use_yn = 'Y'
        """;
    return jdbc.update(sql, new MapSqlParameterSource().addValue("match_id", matchId));
  }

  public void insertOverride(MatchAdminRow match, String status, String menuName, String imageUrl, String note, String username) {
    String sql = """
        INSERT INTO fp_343_season_store_match_override
          (ingredient_id, store_id, restaurant_id, place_id, override_status_cd,
           representative_menu_name, representative_image_url, note, created_by)
        VALUES
          (:ingredient_id, :store_id, :restaurant_id, :place_id, :status,
           :menu_name, :image_url, :note, :created_by)
        """;
    jdbc.update(
        sql,
        new MapSqlParameterSource()
            .addValue("ingredient_id", match.ingredientId(), Types.VARCHAR)
            .addValue("store_id", match.storeId(), Types.INTEGER)
            .addValue("restaurant_id", match.restaurantId(), Types.BIGINT)
            .addValue("place_id", match.placeId(), Types.VARCHAR)
            .addValue("status", status, Types.VARCHAR)
            .addValue("menu_name", blankToNull(menuName), Types.VARCHAR)
            .addValue("image_url", blankToNull(imageUrl), Types.VARCHAR)
            .addValue("note", blankToNull(note), Types.VARCHAR)
            .addValue("created_by", blankToNull(username), Types.VARCHAR)
    );
  }

  private MapSqlParameterSource keywordSearchParams(
      String ingredientId,
      String keywordType,
      String keyword,
      int limit,
      int offset
  ) {
    String normalizedKeyword = blankToNull(keyword);
    return new MapSqlParameterSource()
        .addValue("ingredient_id", blankToNull(ingredientId), Types.VARCHAR)
        .addValue("keyword_type", blankToNull(keywordType), Types.VARCHAR)
        .addValue("keyword", normalizedKeyword, Types.VARCHAR)
        .addValue("keyword_like", normalizedKeyword == null ? null : "%" + normalizedKeyword + "%", Types.VARCHAR)
        .addValue("limit", limit)
        .addValue("offset", offset);
  }

  private MapSqlParameterSource keywordWriteParams(
      String ingredientId,
      String keyword,
      String keywordType,
      BigDecimal matchWeight,
      boolean exactMatch,
      String description
  ) {
    return new MapSqlParameterSource()
        .addValue("ingredient_id", ingredientId, Types.VARCHAR)
        .addValue("keyword", keyword, Types.VARCHAR)
        .addValue("keyword_type", keywordType, Types.VARCHAR)
        .addValue("match_weight", matchWeight)
        .addValue("exact_match_yn", exactMatch ? "Y" : "N", Types.CHAR)
        .addValue("description", blankToNull(description), Types.VARCHAR);
  }

  private MapSqlParameterSource matchSearchParams(
      String ingredientId,
      String status,
      String source,
      String keyword,
      int limit,
      int offset
  ) {
    String normalizedKeyword = blankToNull(keyword);
    return new MapSqlParameterSource()
        .addValue("ingredient_id", blankToNull(ingredientId), Types.VARCHAR)
        .addValue("status", blankToNull(status), Types.VARCHAR)
        .addValue("source", blankToNull(source), Types.VARCHAR)
        .addValue("keyword", normalizedKeyword, Types.VARCHAR)
        .addValue("keyword_like", normalizedKeyword == null ? null : "%" + normalizedKeyword + "%", Types.VARCHAR)
        .addValue("limit", limit)
        .addValue("offset", offset);
  }

  private MatchAdminRow mapMatchAdminRow(java.sql.ResultSet rs) throws java.sql.SQLException {
    return new MatchAdminRow(
        rs.getObject("match_id", Long.class),
        rs.getString("ingredient_id"),
        rs.getString("ingredient_nm"),
        rs.getObject("store_id", Integer.class),
        rs.getObject("restaurant_id", Long.class),
        rs.getString("place_id"),
        rs.getString("store_name"),
        rs.getString("representative_menu_name"),
        rs.getObject("match_score", BigDecimal.class),
        rs.getObject("season_score", Integer.class),
        rs.getInt("evidence_count"),
        rs.getString("match_status_cd"),
        rs.getString("match_source_cd"),
        rs.getString("matched_keywords"),
        rs.getObject("generated_at", OffsetDateTime.class),
        rs.getObject("expires_at", OffsetDateTime.class)
    );
  }

  private String blankToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  public record KeywordRow(
      Long keywordId,
      String ingredientId,
      String ingredientName,
      String keyword,
      String keywordType,
      BigDecimal matchWeight,
      boolean exactMatch,
      String description,
      String useYn,
      OffsetDateTime createdAt,
      OffsetDateTime updatedAt
  ) {}

  public record MatchAdminRow(
      Long matchId,
      String ingredientId,
      String ingredientName,
      Integer storeId,
      Long restaurantId,
      String placeId,
      String storeName,
      String representativeMenuName,
      BigDecimal matchScore,
      Integer seasonScore,
      int evidenceCount,
      String matchStatus,
      String matchSource,
      String matchedKeywords,
      OffsetDateTime generatedAt,
      OffsetDateTime expiresAt
  ) {}

  public record EvidenceRow(
      Long evidenceId,
      String targetType,
      String targetId,
      String matchedField,
      String matchedKeyword,
      String matchedText,
      BigDecimal fieldWeight,
      BigDecimal keywordWeight,
      BigDecimal score,
      OffsetDateTime createdAt
  ) {}
}
