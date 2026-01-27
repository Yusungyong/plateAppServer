// src/main/java/com/plateapp/plate_main/video/repository/Fp303WatchHistoryRepository.java
package com.plateapp.plate_main.video.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.plateapp.plate_main.video.entity.Fp303WatchHistory;

public interface Fp303WatchHistoryRepository
        extends JpaRepository<Fp303WatchHistory, Long> {

    @Modifying
    @Query("delete from Fp303WatchHistory h where h.storeId = :storeId")
    int deleteByStoreId(@Param("storeId") Long storeId);
}
