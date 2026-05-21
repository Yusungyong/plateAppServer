package com.plateapp.plate_main.restaurant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.Getter;

@Entity
@Table(name = "restaurants")
@Getter
public class Restaurant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "address", nullable = false, length = 300)
    private String address;

    @Column(name = "phone", length = 40)
    private String phone;

    @Column(name = "business_hours", length = 200)
    private String businessHours;

    @Column(name = "introduction", columnDefinition = "text")
    private String introduction;

    @Column(name = "exposure_status", nullable = false, length = 30)
    private String exposureStatus;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Restaurant() {
    }

    public static Restaurant create(
            String title,
            String address,
            String phone,
            String businessHours,
            String introduction,
            String exposureStatus
    ) {
        Restaurant restaurant = new Restaurant();
        restaurant.update(title, address, phone, businessHours, introduction, exposureStatus);
        return restaurant;
    }

    public void update(
            String title,
            String address,
            String phone,
            String businessHours,
            String introduction,
            String exposureStatus
    ) {
        this.title = title;
        this.address = address;
        this.phone = phone;
        this.businessHours = businessHours;
        this.introduction = introduction;
        this.exposureStatus = exposureStatus;
    }

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
