package com.plateapp.plate_main.seasonal.base;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "fp_340")
@Getter
public class Fp340SeasonalBaseFood {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "seasonal_term", nullable = false, length = 20)
    private String seasonalTerm;

    @Column(name = "month", nullable = false)
    private Integer month;

    @Column(name = "category", nullable = false, length = 20)
    private String category;

    @Column(name = "food_name", nullable = false, length = 50)
    private String foodName;

    @Column(name = "card_image_url", length = 1024)
    private String cardImageUrl;

    @Column(name = "card_image_mobile_url", length = 1024)
    private String cardImageMobileUrl;

    protected Fp340SeasonalBaseFood() {
    }

    public void replaceCardImages(String cardImageUrl, String cardImageMobileUrl) {
        this.cardImageUrl = cardImageUrl;
        this.cardImageMobileUrl = cardImageMobileUrl;
    }

    public void clearDesktopCardImage() {
        this.cardImageUrl = null;
    }

    public void clearMobileCardImage() {
        this.cardImageMobileUrl = null;
    }
}
