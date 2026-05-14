package com.plateapp.plate_main.home.dto;

public record HomeContentPrimaryImage(
        Integer imageId,
        String thumbnailUrl,
        String imageUrl,
        Double aspectRatio
) {
}
