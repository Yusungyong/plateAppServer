package com.plateapp.plate_main.recipe.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.plateapp.plate_main.recipe.entity.Fp505RecipeTag;

public interface RecipeTagRepository extends JpaRepository<Fp505RecipeTag, Long> {

  @Query("""
      select t
      from Fp505RecipeTag t
      join Fp506RecipeTagMap m on m.tagId = t.id
      where m.recipeId = :recipeId
      order by t.id asc
  """)
  List<Fp505RecipeTag> findByRecipeId(@Param("recipeId") Long recipeId);
}
