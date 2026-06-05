package com.plateapp.plate_main.home.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.plateapp.plate_main.home.entity.Fp376HomeImpression;

public interface Fp376HomeImpressionRepository extends JpaRepository<Fp376HomeImpression, Long> {

    @Query("""
        select distinct i.storeId
        from Fp376HomeImpression i
        where i.username = :username
          and i.contentType = 'VIDEO'
          and i.storeId is not null
          and i.impressedAt >= :since
    """)
    List<Integer> findRecentVideoStoreIdsByUsername(
            @Param("username") String username,
            @Param("since") LocalDateTime since
    );

    @Query("""
        select distinct i.storeId
        from Fp376HomeImpression i
        where i.guestId = :guestId
          and i.contentType = 'VIDEO'
          and i.storeId is not null
          and i.impressedAt >= :since
    """)
    List<Integer> findRecentVideoStoreIdsByGuestId(
            @Param("guestId") String guestId,
            @Param("since") LocalDateTime since
    );

    @Query("""
        select distinct i.feedNo
        from Fp376HomeImpression i
        where i.username = :username
          and i.contentType = 'IMAGE'
          and i.feedNo is not null
          and i.impressedAt >= :since
    """)
    List<Integer> findRecentImageFeedNosByUsername(
            @Param("username") String username,
            @Param("since") LocalDateTime since
    );

    @Query("""
        select distinct i.feedNo
        from Fp376HomeImpression i
        where i.guestId = :guestId
          and i.contentType = 'IMAGE'
          and i.feedNo is not null
          and i.impressedAt >= :since
    """)
    List<Integer> findRecentImageFeedNosByGuestId(
            @Param("guestId") String guestId,
            @Param("since") LocalDateTime since
    );
}
