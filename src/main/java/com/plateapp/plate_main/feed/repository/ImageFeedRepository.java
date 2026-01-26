// src/main/java/com/plateapp/plate_main/feed/repository/ImageFeedRepository.java
package com.plateapp.plate_main.feed.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.plateapp.plate_main.feed.entity.Fp400ImageFeed;

public interface ImageFeedRepository extends JpaRepository<Fp400ImageFeed, Integer> {

  @EntityGraph(attributePaths = {"writer"})
  Optional<Fp400ImageFeed> findByFeedIdAndUseYn(Integer feedId, String useYn);

  long countByUsernameAndUseYn(String username, String useYn);

  List<Fp400ImageFeed> findByFeedIdIn(List<Integer> feedIds);
}
