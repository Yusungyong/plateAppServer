package com.plateapp.plate_main.seasonal.controller;

import com.plateapp.plate_main.common.api.ApiResponse;
import com.plateapp.plate_main.seasonal.dto.SeasonalHomeDtos;
import com.plateapp.plate_main.seasonal.service.SeasonalHomeService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/home/seasonal")
public class SeasonalHomeController {

    private final SeasonalHomeService seasonalHomeService;

    public SeasonalHomeController(SeasonalHomeService seasonalHomeService) {
        this.seasonalHomeService = seasonalHomeService;
    }

    @GetMapping
    public ApiResponse<SeasonalHomeDtos.SeasonalHomeResponse> getSeasonalHome(
            @RequestParam(required = false) String basis,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer selectedFoodId,
            @RequestParam(required = false) Boolean isGuest,
            @RequestParam(required = false) String guestId
    ) {
        return ApiResponse.ok(seasonalHomeService.getSeasonalHome(basis, month, date, selectedFoodId, isGuest, guestId));
    }

    @GetMapping("/{foodId}")
    public ApiResponse<SeasonalHomeDtos.SeasonalHomeResponse> getSeasonalHomeDetail(
            @PathVariable Integer foodId,
            @RequestParam(required = false) String basis,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Boolean isGuest,
            @RequestParam(required = false) String guestId
    ) {
        return ApiResponse.ok(seasonalHomeService.getSeasonalHomeByFoodId(foodId, basis, month, date, isGuest, guestId));
    }
}