package com.plateapp.plate_main.like.repository;

import com.plateapp.plate_main.like.entity.ImageFeedLike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ImageFeedLikeRepository extends JpaRepository<ImageFeedLike, ImageFeedLike.ImageFeedLikeId> {

    boolean existsByImageFeedIdAndUserId(Integer imageFeedId, String userId);

    Optional<ImageFeedLike> findByImageFeedIdAndUserId(Integer imageFeedId, String userId);

    long countByImageFeedId(Integer imageFeedId);

    @Query("SELECT l FROM ImageFeedLike l WHERE l.imageFeedId = :imageFeedId ORDER BY l.createdAt DESC")
    Page<ImageFeedLike> findByImageFeedIdOrderByCreatedAtDesc(@Param("imageFeedId") Integer imageFeedId, Pageable pageable);
}
