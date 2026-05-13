package com.plateapp.plate_main.recommendation.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.plateapp.plate_main.recommendation.entity.Fp370VideoEvent;

public interface Fp370VideoEventRepository extends JpaRepository<Fp370VideoEvent, Long> {

    Optional<Fp370VideoEvent> findByEventUid(String eventUid);

    boolean existsByEventUid(String eventUid);

    @Query("""
        select distinct e.storeId
        from Fp370VideoEvent e
        where e.username = :username
          and e.eventType in :eventTypes
          and e.serverEventAt >= :since
    """)
    List<Integer> findRecentStoreIdsByUsername(
            @Param("username") String username,
            @Param("eventTypes") Collection<String> eventTypes,
            @Param("since") LocalDateTime since
    );

    @Query("""
        select distinct e.storeId
        from Fp370VideoEvent e
        where e.guestId = :guestId
          and e.eventType in :eventTypes
          and e.serverEventAt >= :since
    """)
    List<Integer> findRecentStoreIdsByGuestId(
            @Param("guestId") String guestId,
            @Param("eventTypes") Collection<String> eventTypes,
            @Param("since") LocalDateTime since
    );

    @Query("""
        select e.storeId as storeId, e.eventType as eventType, count(e) as cnt
        from Fp370VideoEvent e
        where e.storeId in :storeIds
          and e.serverEventAt >= :since
        group by e.storeId, e.eventType
    """)
    List<StoreEventCount> countByStoreIdsSince(
            @Param("storeIds") Collection<Integer> storeIds,
            @Param("since") LocalDateTime since
    );

    interface StoreEventCount {
        Integer getStoreId();
        String getEventType();
        Long getCnt();
    }
}
