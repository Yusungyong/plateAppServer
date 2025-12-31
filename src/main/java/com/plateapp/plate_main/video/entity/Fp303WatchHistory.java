// src/main/java/com/plateapp/plate_main/video/entity/Fp303WatchHistory.java
package com.plateapp.plate_main.video.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "fp_303")
public class Fp303WatchHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;                 // bigserial PK

    @Column(name = "username", nullable = false, length = 50)
    private String username;         // NOT NULL

    @Column(name = "store_id", nullable = false)
    private Long storeId;            // int8

    @Column(name = "watched_at",
            nullable = false,
            insertable = false,
            updatable = false)
    private LocalDateTime watchedAt; // timestamp, DEFAULT CURRENT_TIMESTAMP

    @Column(name = "guest_id", length = 100)
    private String guestId;          // nullable

    @Column(name = "is_guest", nullable = false)
    private Boolean isGuest;         // bool, DEFAULT false
}
