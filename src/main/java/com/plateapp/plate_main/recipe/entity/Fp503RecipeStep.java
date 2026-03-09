package com.plateapp.plate_main.recipe.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "fp_503")
public class Fp503RecipeStep {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "recipe_id", nullable = false)
  private Long recipeId;

  @Column(name = "step_no", nullable = false)
  private Integer stepNo;

  @Column(name = "title", length = 255)
  private String title;

  @Column(name = "description", nullable = false, columnDefinition = "text")
  private String description;

  @Column(name = "image_url", length = 1024)
  private String imageUrl;

  public Long getId() { return id; }

  public Long getRecipeId() { return recipeId; }
  public void setRecipeId(Long recipeId) { this.recipeId = recipeId; }

  public Integer getStepNo() { return stepNo; }
  public void setStepNo(Integer stepNo) { this.stepNo = stepNo; }

  public String getTitle() { return title; }
  public void setTitle(String title) { this.title = title; }

  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }

  public String getImageUrl() { return imageUrl; }
  public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
