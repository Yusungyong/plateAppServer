package com.plateapp.plate_main.recommendation.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.plateapp.plate_main.recommendation.entity.Fp373HomeRecommendationCandidate;

public interface Fp373HomeRecommendationCandidateRepository
        extends JpaRepository<Fp373HomeRecommendationCandidate, Long> {
}
