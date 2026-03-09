package com.plateapp.plate_main.recipe.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "fp_504")
public class Fp504RecipeIngredient {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Column(name = "recipe_id", nullable = false)
  private Long recipeId;

  @Column(name = "name", nullable = false, length = 255)
  private String name;

  @Column(name = "quantity", length = 100)
  private String quantity;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  public Long getId() { return id; }

  public Long getRecipeId() { return recipeId; }
  public void setRecipeId(Long recipeId) { this.recipeId = recipeId; }

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }

  public String getQuantity() { return quantity; }
  public void setQuantity(String quantity) { this.quantity = quantity; }

  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
