// src/main/java/com/plateapp/plate_main/video/repository/Fp303WatchHistoryRepository.java
package com.plateapp.plate_main.video.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.plateapp.plate_main.video.entity.Fp303WatchHistory;

public interface Fp303WatchHistoryRepository
        extends JpaRepository<Fp303WatchHistory, Long> {
}
