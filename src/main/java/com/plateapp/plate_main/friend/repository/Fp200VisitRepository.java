package com.plateapp.plate_main.friend.repository;

import com.plateapp.plate_main.friend.entity.Fp200Visit;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface Fp200VisitRepository extends JpaRepository<Fp200Visit, Integer> {

    @Query(value = """
        SELECT
          v.id                 AS id,
          v.friend_name        AS friendName,
          v.store_id           AS storeId,
          COALESCE(v.store_name, s.store_name) AS storeName,
          COALESCE(v.address, s.address)       AS address,
          v.memo               AS memo,
          v.visit_date         AS visitDate,
          v.created_at         AS createdAt,
          s.thumbnail          AS thumbnail
        FROM fp_200 v
        LEFT JOIN fp_300 s ON s.store_id = v.store_id
        WHERE v.username = :username
          AND (:cursor IS NULL OR v.id < :cursor)
        ORDER BY v.id DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<VisitRow> findVisits(
            @Param("username") String username,
            @Param("cursor") Integer cursor,
            @Param("limit") int limit
    );

    @Query(value = """
        SELECT
          v.id                 AS id,
          v.friend_name        AS friendName,
          v.store_id           AS storeId,
          COALESCE(v.store_name, s.store_name) AS storeName,
          COALESCE(v.address, s.address)       AS address,
          v.memo               AS memo,
          v.visit_date         AS visitDate,
          v.created_at         AS createdAt,
          s.thumbnail          AS thumbnail
        FROM fp_200 v
        LEFT JOIN fp_300 s ON s.store_id = v.store_id
        WHERE v.username = :username
          AND v.friend_name = :friendName
          AND (:cursor IS NULL OR v.id < :cursor)
        ORDER BY v.id DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<VisitRow> findFriendVisits(
            @Param("username") String username,
            @Param("friendName") String friendName,
            @Param("cursor") Integer cursor,
            @Param("limit") int limit
    );

    @Query(value = """
        SELECT
          v.store_id          AS storeId,
          COALESCE(v.store_name, s.store_name) AS storeName,
          COALESCE(v.address, s.address)       AS address,
          s.place_id          AS placeId,
          s.thumbnail         AS thumbnail,
          COUNT(*)            AS visitCount,
          MAX(v.created_at)   AS lastVisitedAt
        FROM fp_200 v
        LEFT JOIN fp_300 s ON s.store_id = v.store_id
        WHERE v.username = :username
        GROUP BY v.store_id, v.store_name, v.address, s.store_name, s.address, s.thumbnail, s.place_id
        ORDER BY MAX(v.created_at) DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<RecentStoreRow> findRecentStores(
            @Param("username") String username,
            @Param("limit") int limit
    );

    @Query(value = """
        SELECT
          v.store_id    AS storeId,
          v.friend_name AS friendName,
          v.visit_date  AS visitDate,
          v.created_at  AS createdAt
        FROM fp_200 v
        WHERE v.username = :username
          AND v.store_id IN (:storeIds)
        ORDER BY v.store_id, v.created_at DESC
        """, nativeQuery = true)
    List<RecentFriendVisitRow> findFriendVisitsForStores(
            @Param("username") String username,
            @Param("storeIds") List<Integer> storeIds
    );

    interface RecentStoreRow {
        Integer getStoreId();
        String getStoreName();
        String getAddress();
        String getPlaceId();
        String getThumbnail();
        Long getVisitCount();
        LocalDateTime getLastVisitedAt();
    }

    interface RecentFriendVisitRow {
        Integer getStoreId();
        String getFriendName();
        LocalDate getVisitDate();
        LocalDateTime getCreatedAt();
    }

    @Query(value = """
        SELECT
          v.store_id           AS storeId,
          COALESCE(v.store_name, s.store_name) AS storeName,
          v.friend_name        AS friendName,
          v.memo               AS memo,
          v.visit_date         AS visitDate,
          v.created_at         AS createdAt,
          u.nick_name          AS nickname,
          u.profile_image_url  AS profileImageUrl
        FROM fp_200 v
        LEFT JOIN fp_100 u ON u.username = v.friend_name
        LEFT JOIN fp_300 s ON s.store_id = v.store_id
        WHERE v.username = :username
          AND v.store_id = :storeId
        ORDER BY v.created_at DESC
        """, nativeQuery = true)
    List<StoreFriendVisitRow> findFriendVisitsByStore(
            @Param("username") String username,
            @Param("storeId") Integer storeId
    );

    @Query(value = """
        SELECT
          v.id                 AS id,
          v.friend_name        AS friendName,
          v.store_id           AS storeId,
          COALESCE(v.store_name, s.store_name) AS storeName,
          COALESCE(v.address, s.address)       AS address,
          v.memo               AS memo,
          v.visit_date         AS visitDate,
          v.created_at         AS createdAt,
          s.thumbnail          AS thumbnail
        FROM fp_200 v
        LEFT JOIN fp_300 s ON s.store_id = v.store_id
        WHERE v.username = :username
          AND (:fromDate IS NULL OR v.visit_date >= :fromDate)
          AND (:toDate IS NULL OR v.visit_date <= :toDate)
        ORDER BY v.visit_date ASC, v.id ASC
        """, nativeQuery = true)
    List<ScheduledVisitRow> findScheduledVisits(
            @Param("username") String username,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    @Modifying
    @Query("delete from Fp200Visit v where v.storeId = :storeId")
    int deleteByStoreId(@Param("storeId") Integer storeId);

    @Modifying
    @Query("delete from Fp200Visit v where v.feedId = :feedId")
    int deleteByFeedId(@Param("feedId") Long feedId);

    interface ScheduledVisitRow {
        Integer getId();
        String getFriendName();
        Integer getStoreId();
        String getStoreName();
        String getMemo();
        LocalDate getVisitDate();
        LocalDateTime getCreatedAt();
        String getAddress();
        String getThumbnail();
    }

    interface StoreFriendVisitRow {
        Integer getStoreId();
        String getStoreName();
        String getFriendName();
        String getMemo();
        LocalDate getVisitDate();
        LocalDateTime getCreatedAt();
        String getNickname();
        String getProfileImageUrl();
    }

    interface VisitRow {
        Integer getId();
        String getFriendName();
        Integer getStoreId();
        String getStoreName();
        String getAddress();
        String getMemo();
        LocalDate getVisitDate();
        LocalDateTime getCreatedAt();
        String getThumbnail();
    }
}
