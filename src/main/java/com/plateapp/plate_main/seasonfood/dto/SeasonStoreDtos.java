package com.plateapp.plate_main.seasonfood.dto;

import java.math.BigDecimal;
import java.util.List;

public class SeasonStoreDtos {

  private SeasonStoreDtos() {
  }

  public record IngredientStoreResponse(
      IngredientSeasonSummary ingredient,
      List<SeasonStoreItem> items,
      int page,
      int size,
      boolean hasNext
  ) {}

  public record IngredientSeasonSummary(
      String ingredientId,
      String name,
      Integer seasonScore,
      boolean isPeak
  ) {}

  public record SeasonStoreItem(
      Long matchId,
      Integer storeId,
      Long restaurantId,
      String placeId,
      String storeName,
      String address,
      String representativeMenuName,
      String representativeImageUrl,
      List<String> matchedKeywords,
      String reasonText,
      BigDecimal matchScore,
      Integer seasonScore,
      Integer distanceM,
      String matchStatus,
      String matchSource
  ) {}

  public record NearbySeasonStoresResponse(
      int month,
      List<NearbySeasonStoreItem> items
  ) {}

  public record NearbySeasonStoreItem(
      String ingredientId,
      String ingredientName,
      Integer storeId,
      Long restaurantId,
      String placeId,
      String storeName,
      String representativeMenuName,
      String representativeImageUrl,
      Integer seasonScore,
      boolean isPeak,
      Integer distanceM,
      String reasonText
  ) {}

  public record StoreSeasonFoodsResponse(
      Integer storeId,
      List<StoreSeasonFoodItem> items
  ) {}

  public record StoreSeasonFoodItem(
      String ingredientId,
      String ingredientName,
      String representativeMenuName,
      Integer seasonScore,
      boolean isPeak,
      String reasonText
  ) {}

  public record HomeSeasonStoresResponse(
      List<HomeSeasonStoreSection> sections
  ) {}

  public record HomeSeasonStoreSection(
      String sectionId,
      String title,
      String ingredientId,
      List<HomeSeasonStoreItem> items
  ) {}

  public record HomeSeasonStoreItem(
      Long matchId,
      Integer storeId,
      Long restaurantId,
      String placeId,
      String storeName,
      String representativeMenuName,
      String thumbnailUrl,
      Integer distanceM,
      String reasonText
  ) {}
}
