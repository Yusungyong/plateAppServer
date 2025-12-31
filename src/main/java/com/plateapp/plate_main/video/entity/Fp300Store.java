// src/main/java/com/plateapp/platehome/video/entity/Fp300Store.java
package com.plateapp.plate_main.video.entity;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.LocalDate;

import org.hibernate.annotations.JdbcTypeCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "fp_300")
public class Fp300Store {

    @Id
    @Column(name = "store_id")
    // 필요하면 @GeneratedValue 추가 (지금 PK 전략에 따라)
    private Integer storeId;

    @Column(name = "title")
    private String title;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "address")
    private String address;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "updated_at")
    private LocalDate updatedAt;

    @Column(name = "created_at")
    private LocalDate createdAt;

    @Column(name = "thumbnail")
    private String thumbnail;

    @Column(name = "store_name")
    private String storeName;

    @JdbcTypeCode(Types.CHAR)
    @Column(name = "open_yn", length = 1)
    private String openYn;

    @JdbcTypeCode(Types.CHAR)
    @Column(name = "use_yn", length = 1, nullable = false)
    private String useYn;

    @Column(name = "deleted_at")
    private LocalDate deletedAt;

    @Column(name = "place_id")
    private String placeId;

    @Column(name = "video_duration")
    private Integer videoDuration;  // 초 단위

    @Column(name = "mute_yn", length = 1)
    private String muteYn;

    @Column(name = "video_size")
    private BigDecimal videoSize;   // numeric(10,2)
}
