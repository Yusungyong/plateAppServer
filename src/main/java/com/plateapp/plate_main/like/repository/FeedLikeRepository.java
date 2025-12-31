package com.plateapp.plate_main.like.repository;

import com.plateapp.plate_main.like.entity.Fp60FeedLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FeedLikeRepository extends JpaRepository<Fp60FeedLike, Fp60FeedLike.Pk> {
  Optional<Fp60FeedLike> findByUsernameAndFeedIdAndUseYn(String username, Integer feedId, String useYn);
  long countByFeedIdAndUseYn(Integer feedId, String useYn);
}
