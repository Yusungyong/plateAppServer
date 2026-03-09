package com.plateapp.plate_main.recipe.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.plateapp.plate_main.recipe.dto.RecipeDtos;
import com.plateapp.plate_main.recipe.service.RecipeService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/recipes")
public class RecipeController {

  private final RecipeService recipeService;

  @GetMapping("/categories")
  public ResponseEntity<?> categories() {
    List<RecipeDtos.CategoryResponse> items = recipeService.listCategories();
    return ResponseEntity.ok(Map.of("items", items));
  }

  @GetMapping
  public ResponseEntity<?> listRecipes(
      @RequestParam(name = "q", required = false) String q,
      @RequestParam(name = "categoryId", required = false) Long categoryId,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size
  ) {
    RecipeDtos.RecipeListResponse response = recipeService.listRecipes(q, categoryId, sort, page, size);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/search")
  public ResponseEntity<?> searchRecipes(
      @RequestParam(name = "q") String q,
      @RequestParam(name = "categoryId", required = false) Long categoryId,
      @RequestParam(name = "sort", required = false) String sort,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "20") int size
  ) {
    RecipeDtos.RecipeListResponse response = recipeService.listRecipes(q, categoryId, sort, page, size);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/{recipeId}")
  public ResponseEntity<?> getRecipe(@PathVariable("recipeId") Long recipeId) {
    RecipeDtos.RecipeDetailResponse response = recipeService.getRecipeDetail(recipeId);
    return ResponseEntity.ok(response);
  }
}
