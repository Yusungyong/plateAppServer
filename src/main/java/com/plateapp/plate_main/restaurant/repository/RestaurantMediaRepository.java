package com.plateapp.plate_main.restaurant.repository;

import com.plateapp.plate_main.restaurant.entity.RestaurantMedia;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantMediaRepository extends JpaRepository<RestaurantMedia, Long> {

    List<RestaurantMedia> findByRestaurantIdOrderByDisplayOrderAscIdAsc(Long restaurantId);

    List<RestaurantMedia> findByRestaurantIdAndMenuIdIsNullOrderByDisplayOrderAscIdAsc(Long restaurantId);

    List<RestaurantMedia> findByMenuIdOrderByDisplayOrderAscIdAsc(Long menuId);

    List<RestaurantMedia> findByMenuIdInOrderByDisplayOrderAscIdAsc(Collection<Long> menuIds);

    void deleteByRestaurantId(Long restaurantId);
}
