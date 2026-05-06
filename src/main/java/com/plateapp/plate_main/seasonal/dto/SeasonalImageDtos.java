package com.plateapp.plate_main.seasonal.dto;

public class SeasonalImageDtos {
    public record SeasonalSourceFoodImageResponse(
            Integer sourceFoodId,
            String imageType,
            String imageUrl,
            String cardImageUrl,
            String cardImageMobileUrl
    ) {}
}
