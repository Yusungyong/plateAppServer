package com.plateapp.plate_main.seasonfood.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import com.plateapp.plate_main.seasonfood.dto.SeasonStoreDtos.HomeSeasonStoreItem;
import com.plateapp.plate_main.seasonfood.dto.SeasonStoreDtos.HomeSeasonStoreSection;
import com.plateapp.plate_main.seasonfood.dto.SeasonStoreDtos.HomeSeasonStoresResponse;
import com.plateapp.plate_main.seasonfood.dto.SeasonStoreDtos.IngredientSeasonSummary;
import com.plateapp.plate_main.seasonfood.dto.SeasonStoreDtos.IngredientStoreResponse;
import com.plateapp.plate_main.seasonfood.dto.SeasonStoreDtos.NearbySeasonStoreItem;
import com.plateapp.plate_main.seasonfood.dto.SeasonStoreDtos.NearbySeasonStoresResponse;
import com.plateapp.plate_main.seasonfood.dto.SeasonStoreDtos.SeasonStoreItem;
import com.plateapp.plate_main.seasonfood.dto.SeasonStoreDtos.StoreSeasonFoodItem;
import com.plateapp.plate_main.seasonfood.dto.SeasonStoreDtos.StoreSeasonFoodsResponse;
import com.plateapp.plate_main.seasonfood.repository.SeasonStoreMatchRepository;
import com.plateapp.plate_main.seasonfood.repository.SeasonStoreMatchRepository.IngredientSeasonRow;
import com.plateapp.plate_main.seasonfood.repository.SeasonStoreMatchRepository.SeasonStoreMatchRow;

@Service
public class SeasonStoreMatchQueryService {

  private static final int DEFAULT_PAGE = 0;
  private static final int DEFAULT_SIZE = 20;
  private static final int MAX_SIZE = 50;
  private static final int DEFAULT_RADIUS_M = 3000;
  private static final int MAX_RADIUS_M = 50000;
  private static final int DEFAULT_LIMIT = 20;
  private static final int HOME_SECTION_FETCH_MULTIPLIER = 6;
  private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

  private final SeasonStoreMatchRepository repository;

  public SeasonStoreMatchQueryService(SeasonStoreMatchRepository repository) {
    this.repository = repository;
  }

  @Transactional(readOnly = true)
  public IngredientStoreResponse getIngredientStores(
      String ingredientId,
      Integer month,
      Double lat,
      Double lng,
      Integer radiusM,
      String regionId,
      Integer page,
      Integer size
  ) {
    assertMatchReady();
    String resolvedIngredientId = requireText(ingredientId, "ingredientId is required");
    int resolvedMonth = resolveMonth(month);
    validateLocationPair(lat, lng);
    Integer resolvedRadius = normalizeOptionalRadius(radiusM, lat, lng);
    int resolvedPage = normalizePage(page);
    int resolvedSize = normalizeSize(size);
    String resolvedRegionId = blankToNull(regionId);

    IngredientSeasonRow ingredient = repository.findIngredientSeason(resolvedIngredientId, resolvedMonth, resolvedRegionId);
    if (ingredient == null) {
      throw new AppException(ErrorCode.SEASON_FOOD_NOT_FOUND, "Season food ingredient not found");
    }

    List<SeasonStoreMatchRow> rows = repository.findIngredientStores(
        resolvedIngredientId,
        resolvedMonth,
        lat,
        lng,
        resolvedRadius,
        resolvedRegionId,
        resolvedSize + 1,
        resolvedPage * resolvedSize
    );

    boolean hasNext = rows.size() > resolvedSize;
    List<SeasonStoreItem> items = rows.stream()
        .limit(resolvedSize)
        .map(this::toSeasonStoreItem)
        .toList();

    return new IngredientStoreResponse(
        new IngredientSeasonSummary(
            ingredient.ingredientId(),
            ingredient.ingredientName(),
            ingredient.seasonScore(),
            ingredient.isPeak()
        ),
        items,
        resolvedPage,
        resolvedSize,
        hasNext
    );
  }

  @Transactional(readOnly = true)
  public NearbySeasonStoresResponse getNearbyStores(
      Integer month,
      double lat,
      double lng,
      Integer radiusM,
      String categoryId,
      Integer limit
  ) {
    assertMatchReady();
    int resolvedMonth = resolveMonth(month);
    validateLatLng(lat, lng);
    int resolvedRadius = normalizeRadius(radiusM);
    int resolvedLimit = normalizeLimit(limit);

    List<NearbySeasonStoreItem> items = repository.findNearbyStores(
        resolvedMonth,
        lat,
        lng,
        resolvedRadius,
        blankToNull(categoryId),
        resolvedLimit
    ).stream()
        .map(row -> new NearbySeasonStoreItem(
            row.ingredientId(),
            row.ingredientName(),
            row.storeId(),
            row.restaurantId(),
            row.placeId(),
            row.storeName(),
            row.representativeMenuName(),
            row.representativeImageUrl(),
            row.seasonScore(),
            row.isPeak(),
            row.distanceM(),
            row.reasonText()
        ))
        .toList();

    return new NearbySeasonStoresResponse(resolvedMonth, items);
  }

  @Transactional(readOnly = true)
  public StoreSeasonFoodsResponse getStoreSeasonFoods(Integer storeId, Integer month) {
    assertMatchReady();
    if (storeId == null || storeId < 1) {
      throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "storeId must be positive");
    }
    int resolvedMonth = resolveMonth(month);

    List<StoreSeasonFoodItem> items = repository.findStoreSeasonFoods(storeId, resolvedMonth).stream()
        .map(row -> new StoreSeasonFoodItem(
            row.ingredientId(),
            row.ingredientName(),
            row.representativeMenuName(),
            row.seasonScore(),
            row.isPeak(),
            row.reasonText()
        ))
        .toList();

    return new StoreSeasonFoodsResponse(storeId, items);
  }

  @Transactional(readOnly = true)
  public HomeSeasonStoresResponse getHomeSections(Integer month, Double lat, Double lng, Integer limit) {
    assertMatchReady();
    int resolvedMonth = resolveMonth(month);
    validateLocationPair(lat, lng);
    int resolvedLimit = normalizeLimit(limit);
    int fetchLimit = Math.min(resolvedLimit * HOME_SECTION_FETCH_MULTIPLIER, 120);

    List<SeasonStoreMatchRow> rows = repository.findHomeMatches(resolvedMonth, lat, lng, fetchLimit);
    Map<String, HomeSeasonStoreSection> sections = new LinkedHashMap<>();

    for (SeasonStoreMatchRow row : rows) {
      HomeSeasonStoreSection section = sections.computeIfAbsent(
          row.ingredientId(),
          ingredientId -> new HomeSeasonStoreSection(
              "SEASON_" + ingredientId,
              "Season picks for " + row.ingredientName(),
              ingredientId,
              new ArrayList<>()
          )
      );
      if (section.items().size() < resolvedLimit) {
        section.items().add(new HomeSeasonStoreItem(
            row.matchId(),
            row.storeId(),
            row.restaurantId(),
            row.placeId(),
            row.storeName(),
            row.representativeMenuName(),
            row.representativeImageUrl(),
            row.distanceM(),
            row.reasonText()
        ));
      }
    }

    return new HomeSeasonStoresResponse(sections.values().stream()
        .filter(section -> !section.items().isEmpty())
        .toList());
  }

  private SeasonStoreItem toSeasonStoreItem(SeasonStoreMatchRow row) {
    return new SeasonStoreItem(
        row.matchId(),
        row.storeId(),
        row.restaurantId(),
        row.placeId(),
        row.storeName(),
        row.address(),
        row.representativeMenuName(),
        row.representativeImageUrl(),
        parseKeywords(row.matchedKeywords()),
        row.reasonText(),
        row.matchScore(),
        row.seasonScore(),
        row.distanceM(),
        row.matchStatus(),
        row.matchSource()
    );
  }

  private void assertMatchReady() {
    if (!repository.matchTablesReady()) {
      throw new AppException(ErrorCode.SEASON_STORE_MATCH_NOT_READY, "Season store match tables are not ready");
    }
  }

  private int resolveMonth(Integer month) {
    int resolvedMonth = month == null ? LocalDate.now(SERVICE_ZONE).getMonthValue() : month;
    if (resolvedMonth < 1 || resolvedMonth > 12) {
      throw new AppException(ErrorCode.SEASON_FOOD_INVALID_MONTH, "month must be between 1 and 12");
    }
    return resolvedMonth;
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

  private int normalizeLimit(Integer limit) {
    int resolvedLimit = limit == null ? DEFAULT_LIMIT : limit;
    if (resolvedLimit < 1 || resolvedLimit > MAX_SIZE) {
      throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "limit must be between 1 and 50");
    }
    return resolvedLimit;
  }

  private Integer normalizeOptionalRadius(Integer radiusM, Double lat, Double lng) {
    if (lat == null && lng == null) {
      return null;
    }
    return normalizeRadius(radiusM);
  }

  private int normalizeRadius(Integer radiusM) {
    int resolvedRadius = radiusM == null ? DEFAULT_RADIUS_M : radiusM;
    if (resolvedRadius < 1 || resolvedRadius > MAX_RADIUS_M) {
      throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "radiusM must be between 1 and 50000");
    }
    return resolvedRadius;
  }

  private void validateLocationPair(Double lat, Double lng) {
    if ((lat == null) != (lng == null)) {
      throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "lat and lng must be provided together");
    }
    if (lat != null) {
      validateLatLng(lat, lng);
    }
  }

  private void validateLatLng(double lat, double lng) {
    if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
      throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "lat or lng is out of range");
    }
  }

  private String requireText(String value, String message) {
    String normalized = blankToNull(value);
    if (normalized == null) {
      throw new AppException(ErrorCode.COMMON_INVALID_INPUT, message);
    }
    return normalized;
  }

  private String blankToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  private List<String> parseKeywords(String keywords) {
    if (keywords == null || keywords.isBlank()) {
      return List.of();
    }
    return List.of(keywords.split(",")).stream()
        .map(String::trim)
        .filter(value -> !value.isEmpty())
        .toList();
  }
}
