package com.plateapp.plate_main.restaurant.repository;

import com.plateapp.plate_main.restaurant.entity.RestaurantCategory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantCategoryRepository extends JpaRepository<RestaurantCategory, Long> {

    List<RestaurantCategory> findByRestaurantIdOrderByDisplayOrderAscIdAsc(Long restaurantId);

    void deleteByRestaurantId(Long restaurantId);
}
