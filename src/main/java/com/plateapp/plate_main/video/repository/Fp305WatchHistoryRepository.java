package com.plateapp.plate_main.video.repository;

import com.plateapp.plate_main.video.entity.Fp305WatchHistory;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface Fp305WatchHistoryRepository extends JpaRepository<Fp305WatchHistory, Integer> {

    Optional<Fp305WatchHistory> findBySessionIdAndUseYnAndDeletedAtIsNull(String sessionId, String useYn);

    Optional<Fp305WatchHistory> findFirstByUsernameAndStoreIdAndUseYnAndDeletedAtIsNullOrderByTimestampDesc(
            String username,
            Integer storeId,
            String useYn
    );

    @Query("""
        SELECT w FROM Fp305WatchHistory w
        WHERE w.username = :username
          AND w.useYn = 'Y'
          AND w.deletedAt IS NULL
          AND (:completedOnly = false OR w.completionStatus = true)
        ORDER BY w.timestamp DESC
    """)
    Page<Fp305WatchHistory> findByUsernameOrderByTimestampDesc(
            @Param("username") String username,
            @Param("completedOnly") boolean completedOnly,
            Pageable pageable
    );

    @Query("""
        SELECT COUNT(DISTINCT w.username)
        FROM Fp305WatchHistory w
        WHERE w.storeId = :storeId
          AND w.useYn = 'Y'
          AND w.deletedAt IS NULL
    """)
    Long countUniqueViewersByStoreId(@Param("storeId") Integer storeId);

    Long countByStoreIdAndUseYnAndDeletedAtIsNull(Integer storeId, String useYn);

    Long countByStoreIdAndCompletionStatusAndUseYnAndDeletedAtIsNull(
            Integer storeId,
            Boolean completionStatus,
            String useYn
    );

    @Query("""
        SELECT AVG(w.durationWatched)
        FROM Fp305WatchHistory w
        WHERE w.storeId = :storeId
          AND w.useYn = 'Y'
          AND w.deletedAt IS NULL
          AND w.durationWatched IS NOT NULL
    """)
    Double getAverageDurationByStoreId(@Param("storeId") Integer storeId);

    @Modifying
    @Query("""
        UPDATE Fp305WatchHistory w
        SET w.durationWatched = :durationWatched,
            w.videoQuality = :videoQuality,
            w.timestamp = CURRENT_TIMESTAMP
        WHERE w.sessionId = :sessionId
          AND w.useYn = 'Y'
    """)
    int updateProgressBySessionId(
            @Param("sessionId") String sessionId,
            @Param("durationWatched") Integer durationWatched,
            @Param("videoQuality") String videoQuality
    );

    @Modifying
    @Query("""
        UPDATE Fp305WatchHistory w
        SET w.completionStatus = :completionStatus,
            w.durationWatched = :durationWatched,
            w.timestamp = CURRENT_TIMESTAMP
        WHERE w.sessionId = :sessionId
          AND w.useYn = 'Y'
    """)
    int updateCompletionBySessionId(
            @Param("sessionId") String sessionId,
            @Param("completionStatus") Boolean completionStatus,
            @Param("durationWatched") Integer durationWatched
    );
}
