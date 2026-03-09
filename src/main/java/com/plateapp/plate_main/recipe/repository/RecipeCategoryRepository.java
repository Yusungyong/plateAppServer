package com.plateapp.plate_main.recipe.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.plateapp.plate_main.recipe.entity.Fp501RecipeCategory;

public interface RecipeCategoryRepository extends JpaRepository<Fp501RecipeCategory, Long> {

  List<Fp501RecipeCategory> findAllByOrderBySortOrderAscIdAsc();

  @Query("""
      select c
      from Fp501RecipeCategory c
      join Fp502RecipeCategoryMap m on m.categoryId = c.id
      where m.recipeId = :recipeId
      order by c.sortOrder asc, c.id asc
  """)
  List<Fp501RecipeCategory> findByRecipeId(@Param("recipeId") Long recipeId);
}
