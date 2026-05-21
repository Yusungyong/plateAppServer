package com.plateapp.plate_main.restaurant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.Getter;

@Entity
@Table(name = "restaurant_categories")
@Getter
public class RestaurantCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;

    @Column(name = "category_code", nullable = false, length = 50)
    private String categoryCode;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected RestaurantCategory() {
    }

    public static RestaurantCategory create(Long restaurantId, String categoryCode, Integer displayOrder) {
        RestaurantCategory category = new RestaurantCategory();
        category.restaurantId = restaurantId;
        category.categoryCode = categoryCode;
        category.displayOrder = displayOrder == null ? 0 : displayOrder;
        return category;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
