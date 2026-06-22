package com.plateapp.plate_main.admin.seasonal.repository;import com.plateapp.plate_main.admin.seasonal.entity.SeasonalCuration;import org.springframework.data.domain.*;import org.springframework.data.jpa.repository.JpaRepository;
public interface SeasonalCurationRepository extends JpaRepository<SeasonalCuration,Long>{Page<SeasonalCuration> findByStatus(String status,Pageable pageable);}
