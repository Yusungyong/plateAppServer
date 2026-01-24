package com.plateapp.plate_main.like.repository;

import com.plateapp.plate_main.like.entity.StoreLike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StoreLikeRepository extends JpaRepository<StoreLike, StoreLike.StoreLikeId> {

    boolean existsByStoreIdAndUserIdAndUseYn(Integer storeId, String userId, String useYn);

    Optional<StoreLike> findByStoreIdAndUserIdAndUseYn(Integer storeId, String userId, String useYn);

    Optional<StoreLike> findByStoreIdAndUserId(Integer storeId, String userId);

    long countByStoreIdAndUseYn(Integer storeId, String useYn);

    @Query("SELECT l FROM StoreLike l WHERE l.storeId = :storeId AND l.useYn = 'Y' ORDER BY l.createdAt DESC")
    Page<StoreLike> findByStoreIdOrderByCreatedAtDesc(@Param("storeId") Integer storeId, Pageable pageable);
}
