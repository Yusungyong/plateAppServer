// src/main/java/com/plateapp/plate_main/video/repository/Fp310PlaceRepository.java
package com.plateapp.plate_main.video.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.plateapp.plate_main.video.entity.Fp310Place;

public interface Fp310PlaceRepository extends JpaRepository<Fp310Place, Integer> {

    Optional<Fp310Place> findByPlaceIdAndUseYnAndDeletedAtIsNull(
            String placeId,
            String useYn
    );
}
