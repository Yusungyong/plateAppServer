package com.plateapp.plate_main.restaurant.repository;

import com.plateapp.plate_main.restaurant.entity.RestaurantMenu;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantMenuRepository extends JpaRepository<RestaurantMenu, Long> {

    List<RestaurantMenu> findByRestaurantIdOrderByDisplayOrderAscIdAsc(Long restaurantId);

    long countByRestaurantId(Long restaurantId);

    boolean existsByIdAndRestaurantId(Long id, Long restaurantId);

    void deleteByRestaurantId(Long restaurantId);
}
