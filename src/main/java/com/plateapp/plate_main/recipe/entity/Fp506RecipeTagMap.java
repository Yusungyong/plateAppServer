package com.plateapp.plate_main.recipe.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "fp_506")
@IdClass(RecipeTagMapId.class)
public class Fp506RecipeTagMap {

  @Id
  @Column(name = "recipe_id")
  private Long recipeId;

  @Id
  @Column(name = "tag_id")
  private Long tagId;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  public Long getRecipeId() { return recipeId; }
  public void setRecipeId(Long recipeId) { this.recipeId = recipeId; }

  public Long getTagId() { return tagId; }
  public void setTagId(Long tagId) { this.tagId = tagId; }

  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
