package com.plateapp.plate_main.recipe.dto;

import java.time.LocalDateTime;
import java.util.List;

public class RecipeDtos {

  public static class CategoryResponse {
    public Long id;
    public String name;
    public Integer sortOrder;
  }

  public static class TagResponse {
    public Long id;
    public String name;
  }

  public static class IngredientResponse {
    public String name;
    public String quantity;
  }

  public static class StepResponse {
    public Integer stepNo;
    public String title;
    public String description;
    public String imageUrl;
  }

  public static class RecipeListItem {
    public Long id;
    public String title;
    public String summary;
    public String thumbnailUrl;
    public Integer cookTimeMin;
    public String difficulty;
    public Integer likeCount;
    public Integer viewCount;
  }

  public static class RecipeListResponse {
    public int page;
    public int size;
    public long total;
    public List<RecipeListItem> items;
  }

  public static class RecipeDetailResponse {
    public Long id;
    public String title;
    public String summary;
    public String content;
    public String thumbnailUrl;
    public String coverUrl;
    public Integer cookTimeMin;
    public Integer servings;
    public String difficulty;
    public List<CategoryResponse> categories;
    public List<TagResponse> tags;
    public List<IngredientResponse> ingredients;
    public List<StepResponse> steps;
    public Integer likeCount;
    public Integer viewCount;
    public LocalDateTime createdAt;
  }
}
