package com.plateapp.plate_main.admin.storeapproval.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "store_application_categories")
@Getter
public class StoreApplicationCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Column(name = "category_code", nullable = false, length = 50)
    private String categoryCode;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    protected StoreApplicationCategory() {
    }
}
