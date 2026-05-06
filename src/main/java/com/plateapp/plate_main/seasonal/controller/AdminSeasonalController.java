package com.plateapp.plate_main.seasonal.controller;

import com.plateapp.plate_main.seasonal.dto.SeasonalAdminDtos;
import com.plateapp.plate_main.seasonal.dto.SeasonalImageDtos;
import com.plateapp.plate_main.seasonal.service.SeasonalAdminService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/seasonal-foods")
public class AdminSeasonalController {

    private final SeasonalAdminService seasonalAdminService;

    public AdminSeasonalController(SeasonalAdminService seasonalAdminService) {
        this.seasonalAdminService = seasonalAdminService;
    }

    @GetMapping("/source-options")
    public SeasonalAdminDtos.SeasonalFoodSourceOptionListResponse getSeasonalFoodSourceOptions(
            @RequestParam(required = false) Integer month
    ) {
        return seasonalAdminService.getSeasonalFoodSourceOptions(month);
    }

    @PostMapping(value = "/source-options/{sourceFoodId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public SeasonalImageDtos.SeasonalSourceFoodImageResponse createSeasonalSourceNasImage(
            @PathVariable Integer sourceFoodId,
            @RequestParam String imageType,
            @RequestPart("file") org.springframework.web.multipart.MultipartFile file
    ) {
        return seasonalAdminService.createSeasonalSourceNasImage(sourceFoodId, imageType, file);
    }

    @PatchMapping(value = "/source-options/{sourceFoodId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SeasonalImageDtos.SeasonalSourceFoodImageResponse updateSeasonalSourceNasImage(
            @PathVariable Integer sourceFoodId,
            @RequestParam String imageType,
            @RequestPart("file") org.springframework.web.multipart.MultipartFile file
    ) {
        return seasonalAdminService.updateSeasonalSourceNasImage(sourceFoodId, imageType, file);
    }

    @DeleteMapping("/source-options/{sourceFoodId}/images")
    public SeasonalImageDtos.SeasonalSourceFoodImageResponse deleteSeasonalSourceNasImage(
            @PathVariable Integer sourceFoodId,
            @RequestParam String imageType
    ) {
        return seasonalAdminService.deleteSeasonalSourceNasImage(sourceFoodId, imageType);
    }
}
