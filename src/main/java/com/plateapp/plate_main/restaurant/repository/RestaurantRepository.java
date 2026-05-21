package com.plateapp.plate_main.restaurant.repository;

import com.plateapp.plate_main.restaurant.entity.Restaurant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    @Query("""
        select r
        from Restaurant r
        where (:keyword is null
               or lower(r.title) like lower(concat('%', :keyword, '%'))
               or lower(r.address) like lower(concat('%', :keyword, '%')))
          and (:exposureStatus is null or r.exposureStatus = :exposureStatus)
          and (:category is null or exists (
              select 1
              from RestaurantCategory c
              where c.restaurantId = r.id
                and c.categoryCode = :category
          ))
    """)
    Page<Restaurant> searchAdminRestaurants(
            @Param("keyword") String keyword,
            @Param("category") String category,
            @Param("exposureStatus") String exposureStatus,
            Pageable pageable
    );
}
