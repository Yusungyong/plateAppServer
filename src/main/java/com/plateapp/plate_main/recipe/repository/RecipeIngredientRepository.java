package com.plateapp.plate_main.recipe.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.plateapp.plate_main.recipe.entity.Fp504RecipeIngredient;

public interface RecipeIngredientRepository extends JpaRepository<Fp504RecipeIngredient, Long> {

  List<Fp504RecipeIngredient> findByRecipeIdOrderByIdAsc(Long recipeId);
}
