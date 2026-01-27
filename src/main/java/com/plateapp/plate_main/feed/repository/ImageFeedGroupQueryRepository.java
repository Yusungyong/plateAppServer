package com.plateapp.plate_main.feed.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class ImageFeedGroupQueryRepository {

    @PersistenceContext
    private EntityManager em;

    public record GroupRow(
            String groupId,
            String placeId,
            String storeName,
            String address,
            String thumbnail,
            long imageCount,
            Integer latestFeedId,
            LocalDateTime latestCreatedAt
    ) {}

    public List<GroupRow> findRecentGroups(LocalDateTime cursorCreatedAt, Integer cursorFeedId, int limit) {
        String sql = """
            WITH base AS (
              SELECT
                CASE
                  WHEN f.place_id IS NOT NULL THEN 'place:' || f.place_id
                  ELSE 'store:' || COALESCE(f.store_name, '')
                END AS group_id,
                f.place_id,
                f.store_name,
                f.feed_no,
                f.created_at,
                f.thumbnail,
                f.location,
                COALESCE(cardinality(string_to_array(NULLIF(f.images, ''), ',')), 0) AS image_count
              FROM fp_400 f
              WHERE f.use_yn = 'Y'
                AND f.images IS NOT NULL
                AND f.images <> ''
            ),
            latest AS (
              SELECT DISTINCT ON (group_id)
                group_id,
                place_id,
                store_name,
                feed_no AS latest_feed_id,
                created_at AS latest_created_at,
                thumbnail,
                location
              FROM base
              ORDER BY group_id, created_at DESC NULLS LAST, feed_no DESC
            ),
            agg AS (
              SELECT
                group_id,
                place_id,
                store_name,
                SUM(image_count) AS image_count
              FROM base
              GROUP BY group_id, place_id, store_name
            )
            SELECT
              l.group_id,
              l.place_id,
              l.store_name,
              COALESCE(l.location, p.formatted_address) AS address,
              l.thumbnail,
              a.image_count,
              l.latest_feed_id,
              l.latest_created_at
            FROM latest l
            JOIN agg a ON a.group_id = l.group_id
            LEFT JOIN fp_310 p ON p.place_id = l.place_id
            WHERE (:cursorCreatedAt IS NULL OR (
                    l.latest_created_at < :cursorCreatedAt OR
                    (l.latest_created_at = :cursorCreatedAt AND l.latest_feed_id < :cursorFeedId)
                  ))
            ORDER BY l.latest_created_at DESC NULLS LAST, l.latest_feed_id DESC
            LIMIT :limit
            """;

        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(sql)
            .setParameter("cursorCreatedAt", cursorCreatedAt)
            .setParameter("cursorFeedId", cursorFeedId)
            .setParameter("limit", limit)
            .getResultList();

        return rows.stream().map(this::mapRow).toList();
    }

    public List<GroupRow> findNearbyGroups(
            double lat,
            double lng,
            double latDelta,
            double lngDelta,
            int radiusM,
            LocalDateTime cursorCreatedAt,
            Integer cursorFeedId,
            int limit
    ) {
        String sql = """
            WITH base AS (
              SELECT
                f.place_id,
                f.store_name,
                f.feed_no,
                f.created_at,
                f.thumbnail,
                f.location,
                COALESCE(cardinality(string_to_array(NULLIF(f.images, ''), ',')), 0) AS image_count
              FROM fp_400 f
              JOIN fp_310 p ON p.place_id = f.place_id
              WHERE f.use_yn = 'Y'
                AND f.images IS NOT NULL
                AND f.images <> ''
                AND f.place_id IS NOT NULL
                AND p.latitude IS NOT NULL
                AND p.longitude IS NOT NULL
                AND p.latitude BETWEEN (:lat - :latDelta) AND (:lat + :latDelta)
                AND p.longitude BETWEEN (:lng - :lngDelta) AND (:lng + :lngDelta)
            ),
            latest AS (
              SELECT DISTINCT ON (place_id)
                place_id,
                store_name,
                feed_no AS latest_feed_id,
                created_at AS latest_created_at,
                thumbnail,
                location
              FROM base
              ORDER BY place_id, created_at DESC NULLS LAST, feed_no DESC
            ),
            agg AS (
              SELECT
                place_id,
                store_name,
                SUM(image_count) AS image_count
              FROM base
              GROUP BY place_id, store_name
            )
            SELECT
              'place:' || l.place_id AS group_id,
              l.place_id,
              l.store_name,
              COALESCE(l.location, p.formatted_address) AS address,
              l.thumbnail,
              a.image_count,
              l.latest_feed_id,
              l.latest_created_at
            FROM latest l
            JOIN agg a ON a.place_id = l.place_id
            JOIN fp_310 p ON p.place_id = l.place_id
            WHERE 6371000 * 2 * asin(
                    sqrt(
                      pow(sin(radians((p.latitude - :lat) / 2)), 2) +
                      cos(radians(:lat)) * cos(radians(p.latitude)) *
                      pow(sin(radians((p.longitude - :lng) / 2)), 2)
                    )
                  ) <= :radiusM
              AND (:cursorCreatedAt IS NULL OR (
                    l.latest_created_at < :cursorCreatedAt OR
                    (l.latest_created_at = :cursorCreatedAt AND l.latest_feed_id < :cursorFeedId)
                  ))
            ORDER BY l.latest_created_at DESC NULLS LAST, l.latest_feed_id DESC
            LIMIT :limit
            """;

        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createNativeQuery(sql)
            .setParameter("lat", lat)
            .setParameter("lng", lng)
            .setParameter("latDelta", latDelta)
            .setParameter("lngDelta", lngDelta)
            .setParameter("radiusM", radiusM)
            .setParameter("cursorCreatedAt", cursorCreatedAt)
            .setParameter("cursorFeedId", cursorFeedId)
            .setParameter("limit", limit)
            .getResultList();

        return rows.stream().map(this::mapRow).toList();
    }

    private GroupRow mapRow(Object[] row) {
        String groupId = Objects.toString(row[0], null);
        String placeId = Objects.toString(row[1], null);
        String storeName = Objects.toString(row[2], null);
        String address = Objects.toString(row[3], null);
        String thumbnail = Objects.toString(row[4], null);
        long imageCount = row[5] == null ? 0L : ((Number) row[5]).longValue();
        Integer latestFeedId = row[6] == null ? null : ((Number) row[6]).intValue();
        LocalDateTime latestCreatedAt = row[7] == null ? null : ((java.sql.Timestamp) row[7]).toLocalDateTime();
        return new GroupRow(
                groupId,
                placeId,
                storeName,
                address,
                thumbnail,
                imageCount,
                latestFeedId,
                latestCreatedAt
        );
    }
}
