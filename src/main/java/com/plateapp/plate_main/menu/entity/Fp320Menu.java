package com.plateapp.plate_main.menu.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.sql.Types;
import org.hibernate.annotations.JdbcTypeCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "fp_320")
public class Fp320Menu {

    @Id
    @Column(name = "item_id")
    private String itemId;

    @Column(name = "store_id")
    private Integer storeId;

    @Column(name = "item_name")
    private String itemName;

    @Column(name = "price")
    private String price;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "menu_image")
    private String menuImage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @JdbcTypeCode(Types.CHAR)
    @Column(name = "use_yn", length = 1)
    private String useYn;

    @Column(name = "deleted_at")
    private LocalDate deletedAt;

    @Column(name = "feed_id")
    private Integer feedId;

    @Column(name = "menu_title")
    private String menuTitle;

    @Column(name = "place_id")
    private String placeId;

    @Column(name = "store_name")
    private String storeName;
}
