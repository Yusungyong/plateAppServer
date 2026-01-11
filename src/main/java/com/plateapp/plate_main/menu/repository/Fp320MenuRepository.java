package com.plateapp.plate_main.menu.repository;

import com.plateapp.plate_main.menu.entity.Fp320Menu;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface Fp320MenuRepository extends JpaRepository<Fp320Menu, String> {

    @Query("""
        select m
        from Fp320Menu m
        where m.useYn = 'Y'
          and m.deletedAt is null
          and (:placeId is null or m.placeId = :placeId)
          and (:storeName is null or m.storeName = :storeName)
        order by m.createdAt desc nulls last, m.itemId desc
        """)
    List<Fp320Menu> findActiveMenus(
            @Param("placeId") String placeId,
            @Param("storeName") String storeName
    );
}
