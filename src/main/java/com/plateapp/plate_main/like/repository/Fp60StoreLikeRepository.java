package com.plateapp.plate_main.like.repository;

import com.plateapp.plate_main.like.entity.Fp60FeedLike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface Fp60StoreLikeRepository extends JpaRepository<Fp60FeedLike, Fp60FeedLike.Pk> {

    @Query("SELECT COUNT(l) FROM Fp60FeedLike l WHERE l.feedId = :storeId AND l.useYn = 'Y'")
    long countByStoreIdAndActive(@Param("storeId") Integer storeId);

    @Query("SELECT l FROM Fp60FeedLike l WHERE l.username = :username AND l.feedId = :storeId")
    Optional<Fp60FeedLike> findByUsernameAndStoreId(@Param("username") String username, @Param("storeId") Integer storeId);

    @Query("SELECT l FROM Fp60FeedLike l WHERE l.feedId = :storeId AND l.useYn = 'Y'")
    Page<Fp60FeedLike> findActiveByStoreId(@Param("storeId") Integer storeId, Pageable pageable);
}
