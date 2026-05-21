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
@Table(name = "restaurant_media")
@Getter
public class RestaurantMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;

    @Column(name = "menu_id")
    private Long menuId;

    @Column(name = "media_type", nullable = false, length = 20)
    private String mediaType;

    @Column(name = "usage_type", nullable = false, length = 30)
    private String usageType;

    @Column(name = "file_url", nullable = false, columnDefinition = "text")
    private String fileUrl;

    @Column(name = "original_name")
    private String originalName;

    @Column(name = "mime_type", length = 120)
    private String mimeType;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected RestaurantMedia() {
    }

    public static RestaurantMedia create(
            Long restaurantId,
            Long menuId,
            String mediaType,
            String usageType,
            String fileUrl,
            String originalName,
            String mimeType,
            Long fileSizeBytes,
            Integer displayOrder
    ) {
        RestaurantMedia media = new RestaurantMedia();
        media.restaurantId = restaurantId;
        media.menuId = menuId;
        media.mediaType = mediaType;
        media.usageType = usageType;
        media.fileUrl = fileUrl;
        media.originalName = originalName;
        media.mimeType = mimeType;
        media.fileSizeBytes = fileSizeBytes;
        media.displayOrder = displayOrder == null ? 0 : displayOrder;
        return media;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
