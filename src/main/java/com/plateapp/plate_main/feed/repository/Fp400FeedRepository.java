package com.plateapp.plate_main.feed.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.plateapp.plate_main.feed.entity.Fp400Feed;

public interface Fp400FeedRepository extends JpaRepository<Fp400Feed, Integer> {

  @Query("""
    select f
    from Fp400Feed f
    where f.useYn = 'Y'
      and f.images is not null
      and f.images <> ''
    order by f.createdAt desc, f.feedNo desc
  """)
  List<Fp400Feed> findLatestForHome(Pageable pageable);
  
  boolean existsByFeedNoAndUseYn(Integer feedNo, String useYn);
}
