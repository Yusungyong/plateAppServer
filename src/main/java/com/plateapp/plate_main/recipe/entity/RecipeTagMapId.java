package com.plateapp.plate_main.recipe.entity;

import java.io.Serializable;
import java.util.Objects;

public class RecipeTagMapId implements Serializable {
  private Long recipeId;
  private Long tagId;

  public RecipeTagMapId() {}

  public RecipeTagMapId(Long recipeId, Long tagId) {
    this.recipeId = recipeId;
    this.tagId = tagId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    RecipeTagMapId that = (RecipeTagMapId) o;
    return Objects.equals(recipeId, that.recipeId) && Objects.equals(tagId, that.tagId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(recipeId, tagId);
  }
}
