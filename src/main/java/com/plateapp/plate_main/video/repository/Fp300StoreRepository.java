// src/main/java/com/plateapp/plate_main/video/repository/Fp300StoreRepository.java
package com.plateapp.plate_main.video.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.plateapp.plate_main.video.entity.Fp300Store;

public interface Fp300StoreRepository extends JpaRepository<Fp300Store, Integer> {

    /**
     * MyBatis 쿼리 이식:
     * - 공통
     *   use_yn = 'Y'
     *   open_yn = 'Y'
     *   deleted_at IS NULL
     *   file_name IS NOT NULL
     *   차단 유저의 가게 제외 (fp_160)
     *
     * - placeIds 가 비어 있으면 (usePlaceFilter = false)
     *   → 최근 1일 내 시청 이력 존재하면 제외 (fp_303 NOT EXISTS)
     *
     * - placeIds 가 있으면 (usePlaceFilter = true)
     *   → place_id IN (:placeIds)
     *   → 시청 이력 조건은 적용하지 않음
     */
    @Query(
        value = """
            SELECT *
            FROM fp_300 a
            WHERE a.use_yn = 'Y'
              AND a.open_yn = 'Y'
              AND a.deleted_at IS NULL
              AND a.file_name IS NOT NULL
              AND a.username NOT IN (
                  SELECT blocked_username
                  FROM fp_160
                  WHERE blocker_username = :username
              )
              AND (
                  -- placeIds 없음: 최근 1일 시청 이력 제외
                  (:usePlaceFilter = false AND NOT EXISTS (
                      SELECT 1
                      FROM fp_303 h
                      WHERE h.store_id = a.store_id
                        AND h.watched_at >= NOW() - INTERVAL '1 day'
                        AND (
                            (:isGuest = false AND h.username = :username)
                            OR (:isGuest = true AND h.guest_id = :guestId)
                        )
                  ))
                  OR
                  -- placeIds 있음: place_id IN (...)
                  (:usePlaceFilter = true AND a.place_id IN (:placeIds))
              )
            ORDER BY a.created_at DESC
            """,
        countQuery = """
            SELECT COUNT(*)
            FROM fp_300 a
            WHERE a.use_yn = 'Y'
              AND a.open_yn = 'Y'
              AND a.deleted_at IS NULL
              AND a.file_name IS NOT NULL
              AND a.username NOT IN (
                  SELECT blocked_username
                  FROM fp_160
                  WHERE blocker_username = :username
              )
              AND (
                  (:usePlaceFilter = false AND NOT EXISTS (
                      SELECT 1
                      FROM fp_303 h
                      WHERE h.store_id = a.store_id
                        AND h.watched_at >= NOW() - INTERVAL '1 day'
                        AND (
                            (:isGuest = false AND h.username = :username)
                            OR (:isGuest = true AND h.guest_id = :guestId)
                        )
                  ))
                  OR
                  (:usePlaceFilter = true AND a.place_id IN (:placeIds))
              )
            """,
        nativeQuery = true
    )
    Page<Fp300Store> findHomeVideoThumbnails(
            @Param("username") String username,
            @Param("isGuest") boolean isGuest,
            @Param("guestId") String guestId,
            @Param("usePlaceFilter") boolean usePlaceFilter,
            @Param("placeIds") List<String> placeIds,
            Pageable pageable
    );

    @Query(
        value = """
            SELECT s.*
            FROM fp_300 s
            JOIN fp_310 loc
              ON s.place_id = loc.place_id
            WHERE s.use_yn = 'Y'
              AND s.open_yn = 'Y'
              AND s.deleted_at IS NULL
              AND s.file_name IS NOT NULL
              AND loc.latitude IS NOT NULL
              AND loc.longitude IS NOT NULL
              AND (
                  :username IS NULL OR s.username NOT IN (
                      SELECT blocked_username
                      FROM fp_160
                      WHERE blocker_username = :username
                  )
              )
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
              s.created_at DESC
            """,
        countQuery = """
            SELECT COUNT(*)
            FROM fp_300 s
            JOIN fp_310 loc
              ON s.place_id = loc.place_id
            WHERE s.use_yn = 'Y'
              AND s.open_yn = 'Y'
              AND s.deleted_at IS NULL
              AND s.file_name IS NOT NULL
              AND loc.latitude IS NOT NULL
              AND loc.longitude IS NOT NULL
              AND (
                  :username IS NULL OR s.username NOT IN (
                      SELECT blocked_username
                      FROM fp_160
                      WHERE blocker_username = :username
                  )
              )
              AND (
                6371000 * acos(
                    cos(radians(:centerLat)) * cos(radians(loc.latitude))
                  * cos(radians(loc.longitude) - radians(:centerLng))
                  + sin(radians(:centerLat)) * sin(radians(loc.latitude))
                )
              ) <= :radiusMeters
            """,
        nativeQuery = true
    )
    Page<Fp300Store> findHomeVideoThumbnailsNearby(
            @Param("centerLat") double centerLat,
            @Param("centerLng") double centerLng,
            @Param("radiusMeters") double radiusMeters,
            @Param("username") String username,
            Pageable pageable
    );

    // 단일 스토어 조회 (사용중 + 공개 + 삭제 안 된 것만)
    Optional<Fp300Store> findByStoreIdAndUseYnAndOpenYnAndDeletedAtIsNull(
            Integer storeId, String useYn, String openYn
    );

    // placeId 기준 상위 10개
    List<Fp300Store> findTop10ByPlaceIdAndUseYnAndOpenYnAndDeletedAtIsNullOrderByCreatedAtDesc(
            String placeId, String useYn, String openYn
    );

    // placeId 기준, 특정 storeId 제외하고 상위 9개
    List<Fp300Store> findTop9ByPlaceIdAndStoreIdNotAndUseYnAndOpenYnAndDeletedAtIsNullOrderByCreatedAtDesc(
            String placeId, Integer storeId, String useYn, String openYn
    );

    /**
     * 🔹 위도/경도 기준 반경(radiusMeters) 안에 있는 가게들을 거리순으로 조회
     *  - fp_300(영상) + fp_310(좌표) JOIN
     *  - Haversine 거리 공식 사용 (단위: 미터)
     *  - excludeStoreId 가 있으면 해당 store_id는 결과에서 제외
     */
    @Query(
        value = """
            SELECT s.*
            FROM fp_300 s
            JOIN fp_310 loc
              ON s.place_id = loc.place_id
            WHERE s.use_yn = 'Y'
              AND s.open_yn = 'Y'
              AND s.deleted_at IS NULL
              AND s.file_name IS NOT NULL
              AND loc.latitude IS NOT NULL
              AND loc.longitude IS NOT NULL
              AND (:excludeStoreId IS NULL OR s.store_id <> :excludeStoreId)
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
              )
            LIMIT :limit
            """,
        nativeQuery = true
    )
    List<Fp300Store> findNearbyStores(
            @Param("centerLat") double centerLat,
            @Param("centerLng") double centerLng,
            @Param("radiusMeters") double radiusMeters,
            @Param("excludeStoreId") Integer excludeStoreId,
            @Param("limit") int limit
    );

    @Query(value = "select coalesce(max(store_id),0)+1 from fp_300", nativeQuery = true)
    Long nextStoreIdFallback();

    long countByUsernameAndUseYn(String username, String useYn);
}
