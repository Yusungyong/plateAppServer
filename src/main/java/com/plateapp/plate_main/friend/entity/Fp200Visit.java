package com.plateapp.plate_main.friend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "fp_200")
public class Fp200Visit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "username", length = 100, nullable = false)
    private String username;

    @Column(name = "friend_name", length = 100, nullable = false)
    private String friendName;

    @Column(name = "store_id")
    private Integer storeId;

    @Column(name = "store_name", length = 255)
    private String storeName;

    @Column(name = "memo", columnDefinition = "text")
    private String memo;

    @Column(name = "visit_date")
    private LocalDate visitDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "feed_id")
    private Long feedId;
}
