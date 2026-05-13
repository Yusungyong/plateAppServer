package com.plateapp.plate_main.recommendation.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.plateapp.plate_main.recommendation.entity.Fp374RecommendationServing;

public interface Fp374RecommendationServingRepository extends JpaRepository<Fp374RecommendationServing, Long> {

    Optional<Fp374RecommendationServing> findByRequestId(String requestId);
}
