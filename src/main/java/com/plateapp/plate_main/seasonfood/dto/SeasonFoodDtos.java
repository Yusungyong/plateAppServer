package com.plateapp.plate_main.seasonfood.dto;

import java.time.LocalDate;
import java.util.List;

public class SeasonFoodDtos {

  private SeasonFoodDtos() {
  }

  public record SeasonFoodListResponse(
      int month,
      String regionId,
      List<SeasonFoodItem> items,
      int page,
      int size,
      boolean hasNext
  ) {}

  public record SeasonFoodItem(
      String ingredientId,
      String name,
      List<String> aliases,
      CategorySummary category,
      RegionSummary representativeRegion,
      String thumbnailUrl,
      SeasonSummary season,
      MonthScoreSummary monthScore,
      String summary
  ) {}

  public record SeasonFoodDetailResponse(
      String ingredientId,
      String name,
      List<String> aliases,
      CategorySummary category,
      RegionSummary representativeRegion,
      String description,
      String storageTip,
      String thumbnailUrl,
      CurrentMonthScore currentMonthScore,
      List<SeasonDetail> seasons,
      List<DishItem> dishes
  ) {}

  public record CategoryTreeResponse(List<CategoryNode> items) {}

  public record CategoryNode(
      String categoryId,
      String parentCategoryId,
      String name,
      int level,
      List<CategoryNode> children
  ) {}

  public record RegionTreeResponse(List<RegionNode> items) {}

  public record RegionNode(
      String regionId,
      String parentRegionId,
      String name,
      String type,
      List<RegionNode> children
  ) {}

  public record CategorySummary(String categoryId, String name) {}

  public record RegionSummary(String regionId, String name) {}

  public record SeasonSummary(
      String seasonId,
      String displayText,
      String seasonType,
      String confidence,
      int startMonth,
      int endMonth,
      String peakMonthText
  ) {}

  public record MonthScoreSummary(
      int month,
      int seasonScore,
      boolean isPeak,
      String scoreLabel
  ) {}

  public record CurrentMonthScore(
      int month,
      int seasonScore,
      boolean isPeak,
      String scoreLabel,
      String description
  ) {}

  public record SeasonDetail(
      String seasonId,
      RegionSummary region,
      String seasonType,
      int startMonth,
      int endMonth,
      String peakMonthText,
      String displayText,
      String confidence,
      String description,
      List<MonthScoreSummary> monthScores,
      List<ReasonItem> reasons,
      List<SourceItem> sources
  ) {}

  public record ReasonItem(
      String reasonCode,
      String title,
      String description
  ) {}

  public record SourceItem(
      String sourceType,
      String name,
      String url,
      LocalDate checkedAt,
      String reliability
  ) {}

  public record DishItem(
      String dishId,
      String name,
      String cookingType,
      String thumbnailUrl,
      boolean isRepresentative,
      List<Integer> recommendMonths,
      String description
  ) {}
}
