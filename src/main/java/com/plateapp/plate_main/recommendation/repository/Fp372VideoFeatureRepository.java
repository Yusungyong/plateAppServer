package com.plateapp.plate_main.recommendation.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.plateapp.plate_main.recommendation.entity.Fp372VideoFeature;

public interface Fp372VideoFeatureRepository extends JpaRepository<Fp372VideoFeature, Integer> {

    List<Fp372VideoFeature> findByStoreIdIn(Collection<Integer> storeIds);
}
