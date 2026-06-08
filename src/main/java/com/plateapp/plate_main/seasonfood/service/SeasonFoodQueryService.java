package com.plateapp.plate_main.seasonfood.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import com.plateapp.plate_main.seasonfood.dto.SeasonFoodDtos.CategoryNode;
import com.plateapp.plate_main.seasonfood.dto.SeasonFoodDtos.CategorySummary;
import com.plateapp.plate_main.seasonfood.dto.SeasonFoodDtos.CategoryTreeResponse;
import com.plateapp.plate_main.seasonfood.dto.SeasonFoodDtos.CurrentMonthScore;
import com.plateapp.plate_main.seasonfood.dto.SeasonFoodDtos.DishItem;
import com.plateapp.plate_main.seasonfood.dto.SeasonFoodDtos.MonthScoreSummary;
import com.plateapp.plate_main.seasonfood.dto.SeasonFoodDtos.ReasonItem;
import com.plateapp.plate_main.seasonfood.dto.SeasonFoodDtos.RegionNode;
import com.plateapp.plate_main.seasonfood.dto.SeasonFoodDtos.RegionSummary;
import com.plateapp.plate_main.seasonfood.dto.SeasonFoodDtos.RegionTreeResponse;
import com.plateapp.plate_main.seasonfood.dto.SeasonFoodDtos.SeasonDetail;
import com.plateapp.plate_main.seasonfood.dto.SeasonFoodDtos.SeasonFoodDetailResponse;
import com.plateapp.plate_main.seasonfood.dto.SeasonFoodDtos.SeasonFoodItem;
import com.plateapp.plate_main.seasonfood.dto.SeasonFoodDtos.SeasonFoodListResponse;
import com.plateapp.plate_main.seasonfood.dto.SeasonFoodDtos.SeasonSummary;
import com.plateapp.plate_main.seasonfood.dto.SeasonFoodDtos.SourceItem;
import com.plateapp.plate_main.seasonfood.repository.SeasonFoodRepository;
import com.plateapp.plate_main.seasonfood.repository.SeasonFoodRepository.CategoryRow;
import com.plateapp.plate_main.seasonfood.repository.SeasonFoodRepository.DishRow;
import com.plateapp.plate_main.seasonfood.repository.SeasonFoodRepository.IngredientRow;
import com.plateapp.plate_main.seasonfood.repository.SeasonFoodRepository.MonthScoreRow;
import com.plateapp.plate_main.seasonfood.repository.SeasonFoodRepository.ReasonRow;
import com.plateapp.plate_main.seasonfood.repository.SeasonFoodRepository.RegionRow;
import com.plateapp.plate_main.seasonfood.repository.SeasonFoodRepository.SeasonFoodListRow;
import com.plateapp.plate_main.seasonfood.repository.SeasonFoodRepository.SeasonWindowRow;
import com.plateapp.plate_main.seasonfood.repository.SeasonFoodRepository.SourceRow;

@Service
public class SeasonFoodQueryService {

  private static final int DEFAULT_MIN_SCORE = 60;
  private static final int DEFAULT_PAGE = 0;
  private static final int DEFAULT_SIZE = 20;
  private static final int MAX_SIZE = 50;
  private static final String DEFAULT_REGION_ID = "REG_ALL";
  private static final String DEFAULT_CATEGORY_TYPE = "INGREDIENT";
  private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

  private final SeasonFoodRepository repository;

  public SeasonFoodQueryService(SeasonFoodRepository repository) {
    this.repository = repository;
  }

  @Transactional(readOnly = true)
  public SeasonFoodListResponse getSeasonFoods(
      Integer month,
      String categoryId,
      String regionId,
      Integer minScore,
      Integer page,
      Integer size
  ) {
    int resolvedMonth = resolveMonth(month);
    String resolvedCategoryId = blankToNull(categoryId);
    String resolvedRegionId = defaultIfBlank(regionId, DEFAULT_REGION_ID);
    int resolvedMinScore = normalizeMinScore(minScore);
    int resolvedPage = normalizePage(page);
    int resolvedSize = normalizeSize(size);

    validateCategory(resolvedCategoryId);
    validateRegion(resolvedRegionId);

    List<SeasonFoodListRow> rows = repository.findSeasonFoods(
        resolvedMonth,
        resolvedCategoryId,
        resolvedRegionId,
        resolvedMinScore,
        resolvedSize + 1,
        resolvedPage * resolvedSize
    );

    boolean hasNext = rows.size() > resolvedSize;
    List<SeasonFoodItem> items = rows.stream()
        .limit(resolvedSize)
        .map(this::toSeasonFoodItem)
        .toList();

    return new SeasonFoodListResponse(
        resolvedMonth,
        resolvedRegionId,
        items,
        resolvedPage,
        resolvedSize,
        hasNext
    );
  }

  @Transactional(readOnly = true)
  public SeasonFoodDetailResponse getSeasonFoodDetail(String ingredientId, String regionId, Integer month) {
    String resolvedIngredientId = blankToNull(ingredientId);
    if (resolvedIngredientId == null) {
      throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "ingredientId is required");
    }

    String resolvedRegionId = blankToNull(regionId);
    int resolvedMonth = resolveMonth(month);
    validateRegion(resolvedRegionId);

    IngredientRow ingredient = repository.findIngredient(resolvedIngredientId)
        .orElseThrow(() -> new AppException(ErrorCode.SEASON_FOOD_NOT_FOUND, "Season food ingredient not found"));

    List<SeasonWindowRow> seasonRows = repository.findSeasonWindows(resolvedIngredientId, resolvedRegionId);
    List<String> seasonIds = seasonRows.stream()
        .map(SeasonWindowRow::seasonId)
        .toList();

    List<MonthScoreRow> monthScores = repository.findMonthScores(seasonIds);
    Map<String, List<MonthScoreRow>> scoresBySeason = monthScores.stream()
        .collect(Collectors.groupingBy(MonthScoreRow::seasonId, LinkedHashMap::new, Collectors.toList()));
    Map<String, List<ReasonRow>> reasonsBySeason = repository.findReasons(seasonIds).stream()
        .collect(Collectors.groupingBy(ReasonRow::seasonId, LinkedHashMap::new, Collectors.toList()));
    Map<String, List<SourceRow>> sourcesBySeason = repository.findSources(seasonIds).stream()
        .collect(Collectors.groupingBy(SourceRow::seasonId, LinkedHashMap::new, Collectors.toList()));

    Map<String, Integer> seasonOrder = new LinkedHashMap<>();
    for (int i = 0; i < seasonRows.size(); i++) {
      seasonOrder.put(seasonRows.get(i).seasonId(), i);
    }

    CurrentMonthScore currentMonthScore = monthScores.stream()
        .filter(score -> score.month() == resolvedMonth)
        .min(Comparator
            .comparingInt((MonthScoreRow score) -> seasonOrder.getOrDefault(score.seasonId(), Integer.MAX_VALUE))
            .thenComparing(Comparator.comparingInt(MonthScoreRow::seasonScore).reversed()))
        .map(this::toCurrentMonthScore)
        .orElse(null);

    List<SeasonDetail> seasons = seasonRows.stream()
        .map(season -> toSeasonDetail(
            season,
            scoresBySeason.getOrDefault(season.seasonId(), List.of()),
            reasonsBySeason.getOrDefault(season.seasonId(), List.of()),
            sourcesBySeason.getOrDefault(season.seasonId(), List.of())
        ))
        .toList();

    List<DishItem> dishes = repository.findDishes(resolvedIngredientId).stream()
        .map(this::toDishItem)
        .toList();

    return new SeasonFoodDetailResponse(
        ingredient.ingredientId(),
        ingredient.ingredientName(),
        parseAliases(ingredient.aliases()),
        new CategorySummary(ingredient.categoryId(), ingredient.categoryName()),
        toRegionSummary(ingredient.representativeRegionId(), ingredient.representativeRegionName()),
        ingredient.description(),
        ingredient.storageTip(),
        ingredient.thumbnailUrl(),
        currentMonthScore,
        seasons,
        dishes
    );
  }

  @Transactional(readOnly = true)
  public CategoryTreeResponse getCategories(String type) {
    String resolvedType = defaultIfBlank(type, DEFAULT_CATEGORY_TYPE);
    List<CategoryRow> rows = repository.findCategories(resolvedType);

    Map<String, CategoryNode> nodes = new LinkedHashMap<>();
    for (CategoryRow row : rows) {
      nodes.put(row.categoryId(), new CategoryNode(
          row.categoryId(),
          row.parentCategoryId(),
          row.name(),
          row.level(),
          new ArrayList<>()
      ));
    }

    List<CategoryNode> roots = new ArrayList<>();
    for (CategoryRow row : rows) {
      CategoryNode node = nodes.get(row.categoryId());
      CategoryNode parent = nodes.get(row.parentCategoryId());
      if (parent == null) {
        roots.add(node);
      } else {
        parent.children().add(node);
      }
    }

    return new CategoryTreeResponse(roots);
  }

  @Transactional(readOnly = true)
  public RegionTreeResponse getRegions(String type) {
    String resolvedType = blankToNull(type);
    List<RegionRow> rows = repository.findRegions(resolvedType);

    Map<String, RegionNode> nodes = new LinkedHashMap<>();
    for (RegionRow row : rows) {
      nodes.put(row.regionId(), new RegionNode(
          row.regionId(),
          row.parentRegionId(),
          row.name(),
          row.type(),
          new ArrayList<>()
      ));
    }

    List<RegionNode> roots = new ArrayList<>();
    for (RegionRow row : rows) {
      RegionNode node = nodes.get(row.regionId());
      RegionNode parent = nodes.get(row.parentRegionId());
      if (parent == null) {
        roots.add(node);
      } else {
        parent.children().add(node);
      }
    }

    return new RegionTreeResponse(roots);
  }

  private SeasonFoodItem toSeasonFoodItem(SeasonFoodListRow row) {
    return new SeasonFoodItem(
        row.ingredientId(),
        row.ingredientName(),
        parseAliases(row.aliases()),
        new CategorySummary(row.categoryId(), row.categoryName()),
        toRegionSummary(row.representativeRegionId(), row.representativeRegionName()),
        row.thumbnailUrl(),
        new SeasonSummary(
            row.seasonId(),
            row.displayText(),
            row.seasonType(),
            row.confidence(),
            row.startMonth(),
            row.endMonth(),
            row.peakMonthText()
        ),
        new MonthScoreSummary(row.month(), row.seasonScore(), row.isPeak(), row.scoreLabel()),
        row.description()
    );
  }

  private SeasonDetail toSeasonDetail(
      SeasonWindowRow season,
      List<MonthScoreRow> monthScores,
      List<ReasonRow> reasons,
      List<SourceRow> sources
  ) {
    return new SeasonDetail(
        season.seasonId(),
        new RegionSummary(season.regionId(), season.regionName()),
        season.seasonType(),
        season.startMonth(),
        season.endMonth(),
        season.peakMonthText(),
        season.displayText(),
        season.confidence(),
        season.description(),
        monthScores.stream()
            .map(score -> new MonthScoreSummary(score.month(), score.seasonScore(), score.isPeak(), score.scoreLabel()))
            .toList(),
        reasons.stream()
            .map(reason -> new ReasonItem(reason.reasonCode(), reason.title(), reason.description()))
            .toList(),
        sources.stream()
            .map(source -> new SourceItem(
                source.sourceType(),
                source.name(),
                source.url(),
                source.checkedAt(),
                source.reliability()
            ))
            .toList()
    );
  }

  private DishItem toDishItem(DishRow row) {
    return new DishItem(
        row.dishId(),
        row.name(),
        row.cookingType(),
        row.thumbnailUrl(),
        row.isRepresentative(),
        parseRecommendMonths(row.recommendMonths()),
        row.description()
    );
  }

  private CurrentMonthScore toCurrentMonthScore(MonthScoreRow row) {
    return new CurrentMonthScore(
        row.month(),
        row.seasonScore(),
        row.isPeak(),
        row.scoreLabel(),
        row.description()
    );
  }

  private RegionSummary toRegionSummary(String regionId, String regionName) {
    if (regionId == null || regionId.isBlank()) {
      return null;
    }
    return new RegionSummary(regionId, regionName);
  }

  private void validateCategory(String categoryId) {
    if (categoryId != null && !repository.existsCategory(categoryId)) {
      throw new AppException(ErrorCode.SEASON_CATEGORY_NOT_FOUND, "Season food category not found");
    }
  }

  private void validateRegion(String regionId) {
    if (regionId != null && !repository.existsRegion(regionId)) {
      throw new AppException(ErrorCode.SEASON_REGION_NOT_FOUND, "Season region not found");
    }
  }

  private int resolveMonth(Integer month) {
    int resolvedMonth = month == null ? LocalDate.now(SERVICE_ZONE).getMonthValue() : month;
    if (resolvedMonth < 1 || resolvedMonth > 12) {
      throw new AppException(ErrorCode.SEASON_FOOD_INVALID_MONTH, "month must be between 1 and 12");
    }
    return resolvedMonth;
  }

  private int normalizeMinScore(Integer minScore) {
    int resolvedMinScore = minScore == null ? DEFAULT_MIN_SCORE : minScore;
    if (resolvedMinScore < 0 || resolvedMinScore > 100) {
      throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "minScore must be between 0 and 100");
    }
    return resolvedMinScore;
  }

  private int normalizePage(Integer page) {
    int resolvedPage = page == null ? DEFAULT_PAGE : page;
    if (resolvedPage < 0) {
      throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "page must be greater than or equal to 0");
    }
    return resolvedPage;
  }

  private int normalizeSize(Integer size) {
    int resolvedSize = size == null ? DEFAULT_SIZE : size;
    if (resolvedSize < 1 || resolvedSize > MAX_SIZE) {
      throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "size must be between 1 and 50");
    }
    return resolvedSize;
  }

  private String defaultIfBlank(String value, String defaultValue) {
    String normalized = blankToNull(value);
    return normalized == null ? defaultValue : normalized;
  }

  private String blankToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  private List<String> parseAliases(String aliases) {
    if (aliases == null || aliases.isBlank()) {
      return List.of();
    }
    return List.of(aliases.split(",")).stream()
        .map(String::trim)
        .filter(value -> !value.isEmpty())
        .toList();
  }

  private List<Integer> parseRecommendMonths(String recommendMonths) {
    if (recommendMonths == null || recommendMonths.isBlank()) {
      return List.of();
    }
    return List.of(recommendMonths.split(",")).stream()
        .map(String::trim)
        .filter(value -> !value.isEmpty())
        .map(this::parseMonthOrNull)
        .filter(Objects::nonNull)
        .toList();
  }

  private Integer parseMonthOrNull(String value) {
    try {
      int month = Integer.parseInt(value);
      return month >= 1 && month <= 12 ? month : null;
    } catch (NumberFormatException ignored) {
      return null;
    }
  }
}
