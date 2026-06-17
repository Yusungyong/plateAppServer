package com.plateapp.plate_main.admin.storeapproval.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;

@Entity
@Table(name = "store_application_menus")
@Getter
public class StoreApplicationMenu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "price", precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    protected StoreApplicationMenu() {
    }

    public static StoreApplicationMenu create(
            Long applicationId,
            String name,
            BigDecimal price,
            String description,
            Integer displayOrder
    ) {
        StoreApplicationMenu menu = new StoreApplicationMenu();
        menu.applicationId = applicationId;
        menu.name = name;
        menu.price = price;
        menu.description = description;
        menu.displayOrder = displayOrder == null ? 0 : displayOrder;
        return menu;
    }
}
