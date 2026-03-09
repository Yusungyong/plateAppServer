package com.plateapp.plate_main.recipe.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.plateapp.plate_main.recipe.entity.Fp503RecipeStep;

public interface RecipeStepRepository extends JpaRepository<Fp503RecipeStep, Long> {

  List<Fp503RecipeStep> findByRecipeIdOrderByStepNoAsc(Long recipeId);
}
