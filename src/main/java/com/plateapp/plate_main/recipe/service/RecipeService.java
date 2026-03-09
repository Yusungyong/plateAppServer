package com.plateapp.plate_main.recipe.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plateapp.plate_main.recipe.dto.RecipeDtos;
import com.plateapp.plate_main.recipe.entity.Fp500Recipe;
import com.plateapp.plate_main.recipe.entity.Fp501RecipeCategory;
import com.plateapp.plate_main.recipe.entity.Fp503RecipeStep;
import com.plateapp.plate_main.recipe.entity.Fp504RecipeIngredient;
import com.plateapp.plate_main.recipe.entity.Fp505RecipeTag;
import com.plateapp.plate_main.recipe.repository.RecipeCategoryRepository;
import com.plateapp.plate_main.recipe.repository.RecipeIngredientRepository;
import com.plateapp.plate_main.recipe.repository.RecipeRepository;
import com.plateapp.plate_main.recipe.repository.RecipeStepRepository;
import com.plateapp.plate_main.recipe.repository.RecipeTagRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RecipeService {

  private final RecipeRepository recipeRepository;
  private final RecipeCategoryRepository categoryRepository;
  private final RecipeTagRepository tagRepository;
  private final RecipeIngredientRepository ingredientRepository;
  private final RecipeStepRepository stepRepository;

  @Transactional(readOnly = true)
  public List<RecipeDtos.CategoryResponse> listCategories() {
    List<RecipeDtos.CategoryResponse> items = new ArrayList<>();
    for (Fp501RecipeCategory c : categoryRepository.findAllByOrderBySortOrderAscIdAsc()) {
      RecipeDtos.CategoryResponse r = new RecipeDtos.CategoryResponse();
      r.id = c.getId();
      r.name = c.getName();
      r.sortOrder = c.getSortOrder();
      items.add(r);
    }
    return items;
  }

  @Transactional(readOnly = true)
  public RecipeDtos.RecipeListResponse listRecipes(String q, Long categoryId, String sort, int page, int size) {
    int safePage = Math.max(page, 0);
    int safeSize = Math.min(Math.max(size, 1), 50);
    Pageable pageable = PageRequest.of(safePage, safeSize, toSort(sort));

    Page<Fp500Recipe> recipePage = recipeRepository.searchPublished(q, categoryId, pageable);

    List<RecipeDtos.RecipeListItem> items = new ArrayList<>();
    for (Fp500Recipe r : recipePage.getContent()) {
      RecipeDtos.RecipeListItem item = new RecipeDtos.RecipeListItem();
      item.id = r.getId();
      item.title = r.getTitle();
      item.summary = r.getSummary();
      item.thumbnailUrl = r.getThumbnailUrl();
      item.cookTimeMin = r.getCookTimeMin();
      item.difficulty = r.getDifficulty();
      item.likeCount = defaultInt(r.getLikeCount());
      item.viewCount = defaultInt(r.getViewCount());
      items.add(item);
    }

    RecipeDtos.RecipeListResponse response = new RecipeDtos.RecipeListResponse();
    response.page = safePage;
    response.size = safeSize;
    response.total = recipePage.getTotalElements();
    response.items = items;
    return response;
  }

  @Transactional
  public RecipeDtos.RecipeDetailResponse getRecipeDetail(Long recipeId) {
    Fp500Recipe recipe = recipeRepository.findByIdAndIsPublishedTrue(recipeId)
        .orElseThrow(() -> new NoSuchElementException("recipe not found"));

    recipeRepository.incrementViewCount(recipeId);
    int viewCount = defaultInt(recipe.getViewCount()) + 1;

    List<RecipeDtos.CategoryResponse> categories = new ArrayList<>();
    for (Fp501RecipeCategory c : categoryRepository.findByRecipeId(recipeId)) {
      RecipeDtos.CategoryResponse cr = new RecipeDtos.CategoryResponse();
      cr.id = c.getId();
      cr.name = c.getName();
      cr.sortOrder = c.getSortOrder();
      categories.add(cr);
    }

    List<RecipeDtos.TagResponse> tags = new ArrayList<>();
    for (Fp505RecipeTag t : tagRepository.findByRecipeId(recipeId)) {
      RecipeDtos.TagResponse tr = new RecipeDtos.TagResponse();
      tr.id = t.getId();
      tr.name = t.getName();
      tags.add(tr);
    }

    List<RecipeDtos.IngredientResponse> ingredients = new ArrayList<>();
    for (Fp504RecipeIngredient i : ingredientRepository.findByRecipeIdOrderByIdAsc(recipeId)) {
      RecipeDtos.IngredientResponse ir = new RecipeDtos.IngredientResponse();
      ir.name = i.getName();
      ir.quantity = i.getQuantity();
      ingredients.add(ir);
    }

    List<RecipeDtos.StepResponse> steps = new ArrayList<>();
    for (Fp503RecipeStep s : stepRepository.findByRecipeIdOrderByStepNoAsc(recipeId)) {
      RecipeDtos.StepResponse sr = new RecipeDtos.StepResponse();
      sr.stepNo = s.getStepNo();
      sr.title = s.getTitle();
      sr.description = s.getDescription();
      sr.imageUrl = s.getImageUrl();
      steps.add(sr);
    }

    RecipeDtos.RecipeDetailResponse response = new RecipeDtos.RecipeDetailResponse();
    response.id = recipe.getId();
    response.title = recipe.getTitle();
    response.summary = recipe.getSummary();
    response.content = recipe.getContent();
    response.thumbnailUrl = recipe.getThumbnailUrl();
    response.coverUrl = recipe.getCoverUrl();
    response.cookTimeMin = recipe.getCookTimeMin();
    response.servings = recipe.getServings();
    response.difficulty = recipe.getDifficulty();
    response.categories = categories;
    response.tags = tags;
    response.ingredients = ingredients;
    response.steps = steps;
    response.likeCount = defaultInt(recipe.getLikeCount());
    response.viewCount = viewCount;
    response.createdAt = recipe.getCreatedAt();
    return response;
  }

  private Sort toSort(String sort) {
    if (sort == null) {
      return Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("createdAt"));
    }
    String key = sort.trim().toUpperCase(Locale.ROOT);
    if ("POPULAR".equals(key)) {
      return Sort.by(
          Sort.Order.desc("likeCount"),
          Sort.Order.desc("viewCount"),
          Sort.Order.desc("createdAt")
      );
    }
    return Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("createdAt"));
  }

  private int defaultInt(Integer value) {
    return value == null ? 0 : value;
  }
}
