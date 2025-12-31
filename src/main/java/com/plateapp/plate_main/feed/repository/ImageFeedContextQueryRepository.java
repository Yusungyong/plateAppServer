// src/main/java/com/plateapp/plate_main/feed/repository/ImageFeedContextQueryRepository.java
package com.plateapp.plate_main.feed.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class ImageFeedContextQueryRepository {

  @PersistenceContext
  private EntityManager em;

  public static class LatLng {
    private final double lat;
    private final double lng;

    public LatLng(double lat, double lng) {
      this.lat = lat;
      this.lng = lng;
    }
    public double getLat() { return lat; }
    public double getLng() { return lng; }
  }

  public Optional<LatLng> findBaseLatLng(Integer baseFeedId) {
    String sql = """
        SELECT s.latitude, s.longitude
        FROM fp_400 f
        JOIN fp_310 s ON s.place_id = f.place_id
        WHERE f.feed_no = :baseFeedId
          AND f.use_yn = 'Y'
          AND f.place_id IS NOT NULL
          AND s.latitude IS NOT NULL
          AND s.longitude IS NOT NULL
        LIMIT 1
        """;

    @SuppressWarnings("unchecked")
    List<Object[]> rows = em.createNativeQuery(sql)
        .setParameter("baseFeedId", baseFeedId)
        .getResultList();

    if (rows.isEmpty()) return Optional.empty();

    Object[] r = rows.get(0);
    double lat = ((Number) r[0]).doubleValue();
    double lng = ((Number) r[1]).doubleValue();
    return Optional.of(new LatLng(lat, lng));
  }

  public List<Integer> findNearbyFeedIds(
      double baseLat,
      double baseLng,
      double latDelta,
      double lngDelta,
      int radiusM,
      int limit
  ) {
    String sql = """
        SELECT t.feed_no
        FROM (
          SELECT
            f.feed_no,
            f.created_at,
            6371000 * 2 * asin(
              sqrt(
                pow(sin(radians((s.latitude - :baseLat) / 2)), 2) +
                cos(radians(:baseLat)) * cos(radians(s.latitude)) *
                pow(sin(radians((s.longitude - :baseLng) / 2)), 2)
              )
            ) AS dist_m
          FROM fp_400 f
          JOIN fp_310 s ON s.place_id = f.place_id
          WHERE f.use_yn = 'Y'
            AND f.place_id IS NOT NULL
            AND s.latitude IS NOT NULL
            AND s.longitude IS NOT NULL
            -- ✅ bounding box
            AND s.latitude BETWEEN (:baseLat - :latDelta) AND (:baseLat + :latDelta)
            AND s.longitude BETWEEN (:baseLng - :lngDelta) AND (:baseLng + :lngDelta)
        ) t
        WHERE t.dist_m <= :radiusM
        ORDER BY t.dist_m ASC, t.created_at DESC NULLS LAST, t.feed_no DESC
        LIMIT :limit
        """;

    @SuppressWarnings("unchecked")
    List<Number> rows = em.createNativeQuery(sql)
        .setParameter("baseLat", baseLat)
        .setParameter("baseLng", baseLng)
        .setParameter("latDelta", latDelta)
        .setParameter("lngDelta", lngDelta)
        .setParameter("radiusM", radiusM)
        .setParameter("limit", limit)
        .getResultList();

    return rows.stream().map(Number::intValue).toList();
  }
}
