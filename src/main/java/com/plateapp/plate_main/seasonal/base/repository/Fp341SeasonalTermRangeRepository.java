package com.plateapp.plate_main.seasonal.base.repository;

import com.plateapp.plate_main.seasonal.base.Fp341SeasonalTermRange;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface Fp341SeasonalTermRangeRepository extends JpaRepository<Fp341SeasonalTermRange, Integer> {

    @Query("""
        select r
        from Fp341SeasonalTermRange r
        where r.startDate <= :today and r.endDate >= :today
        order by r.startDate asc, r.id asc
    """)
    List<Fp341SeasonalTermRange> findActiveRanges(@Param("today") LocalDate today);
}
