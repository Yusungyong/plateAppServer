package com.plateapp.plate_main.seasonfood.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.plateapp.plate_main.common.api.ApiResponse;
import com.plateapp.plate_main.seasonfood.dto.SeasonStoreDtos.HomeSeasonStoresResponse;
import com.plateapp.plate_main.seasonfood.dto.SeasonStoreDtos.IngredientStoreResponse;
import com.plateapp.plate_main.seasonfood.dto.SeasonStoreDtos.NearbySeasonStoresResponse;
import com.plateapp.plate_main.seasonfood.dto.SeasonStoreDtos.StoreSeasonFoodsResponse;
import com.plateapp.plate_main.seasonfood.service.SeasonStoreMatchQueryService;

@RestController
@RequestMapping("/api")
public class SeasonStoreController {

  private final SeasonStoreMatchQueryService seasonStoreMatchQueryService;

  public SeasonStoreController(SeasonStoreMatchQueryService seasonStoreMatchQueryService) {
    this.seasonStoreMatchQueryService = seasonStoreMatchQueryService;
  }

  @GetMapping("/season-foods/{ingredientId}/stores")
  public ApiResponse<IngredientStoreResponse> getIngredientStores(
      @PathVariable("ingredientId") String ingredientId,
      @RequestParam(name = "month", required = false) Integer month,
      @RequestParam(name = "lat", required = false) Double lat,
      @RequestParam(name = "lng", required = false) Double lng,
      @RequestParam(name = "radiusM", required = false) Integer radiusM,
      @RequestParam(name = "regionId", required = false) String regionId,
      @RequestParam(name = "page", required = false) Integer page,
      @RequestParam(name = "size", required = false) Integer size
  ) {
    return ApiResponse.ok(seasonStoreMatchQueryService.getIngredientStores(
        ingredientId,
        month,
        lat,
        lng,
        radiusM,
        regionId,
        page,
        size
    ));
  }

  @GetMapping("/season-stores/nearby")
  public ApiResponse<NearbySeasonStoresResponse> getNearbySeasonStores(
      @RequestParam(name = "month", required = false) Integer month,
      @RequestParam(name = "lat") double lat,
      @RequestParam(name = "lng") double lng,
      @RequestParam(name = "radiusM", required = false) Integer radiusM,
      @RequestParam(name = "categoryId", required = false) String categoryId,
      @RequestParam(name = "limit", required = false) Integer limit
  ) {
    return ApiResponse.ok(seasonStoreMatchQueryService.getNearbyStores(
        month,
        lat,
        lng,
        radiusM,
        categoryId,
        limit
    ));
  }

  @GetMapping("/stores/{storeId}/season-foods")
  public ApiResponse<StoreSeasonFoodsResponse> getStoreSeasonFoods(
      @PathVariable("storeId") Integer storeId,
      @RequestParam(name = "month", required = false) Integer month
  ) {
    return ApiResponse.ok(seasonStoreMatchQueryService.getStoreSeasonFoods(storeId, month));
  }

  @GetMapping("/season-stores/home")
  public ApiResponse<HomeSeasonStoresResponse> getHomeSeasonStores(
      @RequestParam(name = "month", required = false) Integer month,
      @RequestParam(name = "lat", required = false) Double lat,
      @RequestParam(name = "lng", required = false) Double lng,
      @RequestParam(name = "limit", required = false) Integer limit
  ) {
    return ApiResponse.ok(seasonStoreMatchQueryService.getHomeSections(month, lat, lng, limit));
  }
}
