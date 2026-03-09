package com.plateapp.plate_main.recipe.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import com.plateapp.plate_main.recipe.entity.Fp500Recipe;

import java.util.Optional;

public interface RecipeRepository extends JpaRepository<Fp500Recipe, Long> {

  @Query("""
      select r
      from Fp500Recipe r
      where r.isPublished = true
        and (
          :q is null or :q = '' or
          lower(r.title) like lower(concat('%', :q, '%')) or
          lower(r.summary) like lower(concat('%', :q, '%')) or
          lower(r.content) like lower(concat('%', :q, '%'))
        )
        and (
          :categoryId is null or
          exists (
            select 1
            from Fp502RecipeCategoryMap m
            where m.recipeId = r.id and m.categoryId = :categoryId
          )
        )
  """)
  Page<Fp500Recipe> searchPublished(@Param("q") String q,
                                   @Param("categoryId") Long categoryId,
                                   Pageable pageable);

  Optional<Fp500Recipe> findByIdAndIsPublishedTrue(Long id);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
      update Fp500Recipe r
         set r.viewCount = coalesce(r.viewCount, 0) + 1
       where r.id = :id
  """)
  int incrementViewCount(@Param("id") Long id);
}
