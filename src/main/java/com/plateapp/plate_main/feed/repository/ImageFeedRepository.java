// src/main/java/com/plateapp/plate_main/feed/repository/ImageFeedRepository.java
package com.plateapp.plate_main.feed.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import com.plateapp.plate_main.feed.entity.Fp400ImageFeed;

public interface ImageFeedRepository extends JpaRepository<Fp400ImageFeed, Integer> {

  @EntityGraph(attributePaths = {"writer"})
  Optional<Fp400ImageFeed> findByFeedIdAndUseYn(Integer feedId, String useYn);

  long countByUsernameAndUseYn(String username, String useYn);

  List<Fp400ImageFeed> findByFeedIdIn(List<Integer> feedIds);

  @EntityGraph(attributePaths = {"writer"})
  @Query("""
      SELECT f
      FROM Fp400ImageFeed f
      WHERE f.useYn = 'Y'
        AND f.images IS NOT NULL
        AND f.images <> ''
        AND (
          (:placeId IS NOT NULL AND f.placeId = :placeId)
          OR (:placeId IS NULL AND f.placeId IS NULL AND f.storeName = :storeName)
        )
        AND (
          :cursorCreatedAt IS NULL
          OR f.createdAt < :cursorCreatedAt
          OR (f.createdAt = :cursorCreatedAt AND f.feedId < :cursorFeedId)
        )
      ORDER BY f.createdAt DESC NULLS LAST, f.feedId DESC
      """)
  List<Fp400ImageFeed> findGroupFeeds(
          @Param("placeId") String placeId,
          @Param("storeName") String storeName,
          @Param("cursorCreatedAt") java.time.LocalDateTime cursorCreatedAt,
          @Param("cursorFeedId") Integer cursorFeedId,
          Pageable pageable
  );
}
