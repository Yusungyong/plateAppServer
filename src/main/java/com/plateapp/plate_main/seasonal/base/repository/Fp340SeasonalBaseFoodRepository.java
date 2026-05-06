package com.plateapp.plate_main.seasonal.base.repository;

import com.plateapp.plate_main.seasonal.base.Fp340SeasonalBaseFood;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface Fp340SeasonalBaseFoodRepository extends JpaRepository<Fp340SeasonalBaseFood, Integer> {

    @Query("""
        select f
        from Fp340SeasonalBaseFood f
        where (:month is null or f.month = :month)
          and (:seasonalTerm is null or f.seasonalTerm = :seasonalTerm)
        order by f.month asc, f.seasonalTerm asc, f.category asc, f.foodName asc, f.id asc
    """)
    List<Fp340SeasonalBaseFood> findOptions(@Param("month") Integer month, @Param("seasonalTerm") String seasonalTerm);

    @Query("""
        select f
        from Fp340SeasonalBaseFood f
        where f.seasonalTerm in (
            select r.seasonalTerm
            from Fp341SeasonalTermRange r
            where r.startDate <= :today and r.endDate >= :today
        )
        order by f.month asc, f.seasonalTerm asc, f.category asc, f.foodName asc, f.id asc
    """)
    List<Fp340SeasonalBaseFood> findCurrentSeasonalFoods(@Param("today") LocalDate today);
}
