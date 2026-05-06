package com.plateapp.plate_main.seasonal.dto;

import java.util.List;

public class SeasonalHomeDtos {
    public enum Basis {
        MONTH,
        TERM
    }

    public record SeasonalHomeResponse(
            BasisInfo basisInfo,
            Hero hero,
            List<Chip> chips,
            List<FoodItem> foods
    ) {}

    public record BasisInfo(
            String basis,
            String referenceDate,
            Integer month,
            String seasonalTerm
    ) {}

    public record Hero(
            Integer seasonalFoodId,
            String seasonalTerm,
            Integer month,
            String monthLabel,
            String foodName,
            String category,
            String headline,
            String subcopy,
            String cardImageUrl,
            String cardImageMobileUrl
    ) {}

    public record Chip(
            Integer seasonalFoodId,
            String foodName,
            boolean isActive
    ) {}

    public record FoodItem(
            Integer seasonalFoodId,
            String seasonalTerm,
            Integer month,
            String foodName,
            String category,
            String cardImageUrl,
            String cardImageMobileUrl
    ) {}
}