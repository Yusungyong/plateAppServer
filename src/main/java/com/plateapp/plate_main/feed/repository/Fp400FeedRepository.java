package com.plateapp.plate_main.feed.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.plateapp.plate_main.feed.entity.Fp400Feed;

public interface Fp400FeedRepository extends JpaRepository<Fp400Feed, Integer> {

  @Query("""
    select f
    from Fp400Feed f
    where f.useYn = 'Y'
      and f.images is not null
      and f.images <> ''
      and (
        (:placeId is null and :storeName is null)
        or (:placeId is not null and f.placeId = :placeId)
        or (:placeId is null and :storeName is not null and f.placeId is null and f.storeName = :storeName)
      )
    order by f.createdAt desc, f.feedNo desc
  """)
  List<Fp400Feed> findLatestForHomeByGroup(
      @Param("placeId") String placeId,
      @Param("storeName") String storeName,
      Pageable pageable
  );

  @Query(
    value = """
      SELECT f.*
      FROM fp_400 f
      JOIN fp_310 loc
        ON f.place_id = loc.place_id
      WHERE f.use_yn = 'Y'
        AND f.images IS NOT NULL
        AND f.images <> ''
        AND (
          (:placeId IS NULL AND :storeName IS NULL)
          OR (:placeId IS NOT NULL AND f.place_id = :placeId)
          OR (:placeId IS NULL AND :storeName IS NOT NULL AND f.place_id IS NULL AND f.store_name = :storeName)
        )
        AND loc.latitude IS NOT NULL
        AND loc.longitude IS NOT NULL
        AND (
          6371000 * acos(
              cos(radians(:centerLat)) * cos(radians(loc.latitude))
            * cos(radians(loc.longitude) - radians(:centerLng))
            + sin(radians(:centerLat)) * sin(radians(loc.latitude))
          )
        ) <= :radiusMeters
      ORDER BY
        6371000 * acos(
            cos(radians(:centerLat)) * cos(radians(loc.latitude))
          * cos(radians(loc.longitude) - radians(:centerLng))
          + sin(radians(:centerLat)) * sin(radians(loc.latitude))
        ) ASC,
        f.created_at DESC,
        f.feed_no DESC
      """,
    nativeQuery = true
  )
  List<Fp400Feed> findNearbyForHomeByGroup(
      @Param("centerLat") double centerLat,
      @Param("centerLng") double centerLng,
      @Param("radiusMeters") double radiusMeters,
      @Param("placeId") String placeId,
      @Param("storeName") String storeName,
      Pageable pageable
  );
  
  boolean existsByFeedNoAndUseYn(Integer feedNo, String useYn);
}
