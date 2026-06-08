package com.plateapp.plate_main.seasonfood.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.plateapp.plate_main.common.api.ApiResponse;
import com.plateapp.plate_main.seasonfood.dto.SeasonFoodDtos.CategoryTreeResponse;
import com.plateapp.plate_main.seasonfood.dto.SeasonFoodDtos.RegionTreeResponse;
import com.plateapp.plate_main.seasonfood.dto.SeasonFoodDtos.SeasonFoodDetailResponse;
import com.plateapp.plate_main.seasonfood.dto.SeasonFoodDtos.SeasonFoodListResponse;
import com.plateapp.plate_main.seasonfood.service.SeasonFoodQueryService;

@RestController
@RequestMapping("/api")
public class SeasonFoodController {

  private final SeasonFoodQueryService seasonFoodQueryService;

  public SeasonFoodController(SeasonFoodQueryService seasonFoodQueryService) {
    this.seasonFoodQueryService = seasonFoodQueryService;
  }

  @GetMapping("/season-foods")
  public ApiResponse<SeasonFoodListResponse> getSeasonFoods(
      @RequestParam(name = "month", required = false) Integer month,
      @RequestParam(name = "categoryId", required = false) String categoryId,
      @RequestParam(name = "regionId", required = false) String regionId,
      @RequestParam(name = "minScore", required = false) Integer minScore,
      @RequestParam(name = "page", required = false) Integer page,
      @RequestParam(name = "size", required = false) Integer size
  ) {
    return ApiResponse.ok(seasonFoodQueryService.getSeasonFoods(
        month,
        categoryId,
        regionId,
        minScore,
        page,
        size
    ));
  }

  @GetMapping("/season-foods/{ingredientId}")
  public ApiResponse<SeasonFoodDetailResponse> getSeasonFoodDetail(
      @PathVariable("ingredientId") String ingredientId,
      @RequestParam(name = "regionId", required = false) String regionId,
      @RequestParam(name = "month", required = false) Integer month
  ) {
    return ApiResponse.ok(seasonFoodQueryService.getSeasonFoodDetail(ingredientId, regionId, month));
  }

  @GetMapping("/season-food-categories")
  public ApiResponse<CategoryTreeResponse> getSeasonFoodCategories(
      @RequestParam(name = "type", required = false) String type
  ) {
    return ApiResponse.ok(seasonFoodQueryService.getCategories(type));
  }

  @GetMapping("/season-regions")
  public ApiResponse<RegionTreeResponse> getSeasonRegions(
      @RequestParam(name = "type", required = false) String type
  ) {
    return ApiResponse.ok(seasonFoodQueryService.getRegions(type));
  }
}
