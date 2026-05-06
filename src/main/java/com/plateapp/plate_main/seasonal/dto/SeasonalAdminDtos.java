package com.plateapp.plate_main.seasonal.dto;

import java.time.LocalDate;
import java.util.List;

public class SeasonalAdminDtos {
    public record SeasonalFoodSourceOption(
            Integer sourceFoodId,
            Integer month,
            String seasonalTerm,
            String category,
            String foodName,
            String cardImageUrl,
            String cardImageMobileUrl,
            LocalDate startDate,
            LocalDate endDate
    ) {}

    public record SeasonalFoodSourceOptionListResponse(List<SeasonalFoodSourceOption> items) {}
}
