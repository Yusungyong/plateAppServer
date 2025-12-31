// src/main/java/com/plateapp/plate_main/video/entity/Fp310Place.java
package com.plateapp.plate_main.video.entity;

import java.sql.Types;
import java.time.LocalDate;

import org.hibernate.annotations.JdbcTypeCode;

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
@Table(name = "fp_310")
public class Fp310Place {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;                 // serial4

    @Column(name = "formatted_address")
    private String formattedAddress;    // 전체 주소

    @Column(name = "latitude")
    private Double latitude;           // 위도 (float8)

    @Column(name = "longitude")
    private Double longitude;          // 경도 (float8)

    @Column(name = "place_id")
    private String placeId;            // 지오코딩 place_id

    @JdbcTypeCode(Types.ARRAY)
    @Column(name = "types")
    private String[] types;

    @Column(name = "street_number")
    private String streetNumber;

    @Column(name = "route")
    private String route;

    @Column(name = "locality")
    private String locality;

    @Column(name = "administrative_area_level_1")
    private String administrativeAreaLevel1;

    @Column(name = "administrative_area_level_2")
    private String administrativeAreaLevel2;

    @Column(name = "country")
    private String country;

    @Column(name = "postal_code")
    private String postalCode;

    @JdbcTypeCode(Types.CHAR)
    @Column(name = "use_yn", length = 1, nullable = false)
    private String useYn;              // Y/N

    @Column(name = "deleted_at")
    private LocalDate deletedAt;
}
