package com.plateapp.plate_main.recipe.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "fp_502")
@IdClass(RecipeCategoryMapId.class)
public class Fp502RecipeCategoryMap {

  @Id
  @Column(name = "recipe_id")
  private Long recipeId;

  @Id
  @Column(name = "category_id")
  private Long categoryId;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  public Long getRecipeId() { return recipeId; }
  public void setRecipeId(Long recipeId) { this.recipeId = recipeId; }

  public Long getCategoryId() { return categoryId; }
  public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
