package com.plateapp.plate_main.seasonal.service;

import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import com.plateapp.plate_main.common.file.LocalFileStorageService;
import com.plateapp.plate_main.seasonal.base.Fp340SeasonalBaseFood;
import com.plateapp.plate_main.seasonal.base.Fp341SeasonalTermRange;
import com.plateapp.plate_main.seasonal.base.repository.Fp340SeasonalBaseFoodRepository;
import com.plateapp.plate_main.seasonal.base.repository.Fp341SeasonalTermRangeRepository;
import com.plateapp.plate_main.seasonal.dto.SeasonalAdminDtos;
import com.plateapp.plate_main.seasonal.dto.SeasonalImageDtos;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class SeasonalAdminService {

    private static final String CARD_DESKTOP = "CARD_DESKTOP";
    private static final String CARD_MOBILE = "CARD_MOBILE";

    private final Fp340SeasonalBaseFoodRepository seasonalBaseFoodRepository;
    private final Fp341SeasonalTermRangeRepository seasonalTermRangeRepository;
    private final LocalFileStorageService localFileStorageService;

    public SeasonalAdminService(
            Fp340SeasonalBaseFoodRepository seasonalBaseFoodRepository,
            Fp341SeasonalTermRangeRepository seasonalTermRangeRepository,
            LocalFileStorageService localFileStorageService
    ) {
        this.seasonalBaseFoodRepository = seasonalBaseFoodRepository;
        this.seasonalTermRangeRepository = seasonalTermRangeRepository;
        this.localFileStorageService = localFileStorageService;
    }

    @Transactional(readOnly = true)
    public SeasonalAdminDtos.SeasonalFoodSourceOptionListResponse getSeasonalFoodSourceOptions(Integer month) {
        List<SeasonalAdminDtos.SeasonalFoodSourceOption> items;
        if (month != null) {
            validateMonth(month);
            items = seasonalBaseFoodRepository.findOptions(month, null).stream()
                    .map(food -> new SeasonalAdminDtos.SeasonalFoodSourceOption(
                            food.getId(),
                            food.getMonth(),
                            food.getSeasonalTerm(),
                            food.getCategory(),
                            food.getFoodName(),
                            food.getCardImageUrl(),
                            food.getCardImageMobileUrl(),
                            null,
                            null
                    ))
                    .toList();
            return new SeasonalAdminDtos.SeasonalFoodSourceOptionListResponse(items);
        }

        LocalDate today = LocalDate.now();
        List<Fp341SeasonalTermRange> activeRanges = seasonalTermRangeRepository.findActiveRanges(today);
        if (activeRanges.isEmpty()) {
            items = seasonalBaseFoodRepository.findCurrentSeasonalFoods(today).stream()
                    .map(food -> new SeasonalAdminDtos.SeasonalFoodSourceOption(
                            food.getId(),
                            food.getMonth(),
                            food.getSeasonalTerm(),
                            food.getCategory(),
                            food.getFoodName(),
                            food.getCardImageUrl(),
                            food.getCardImageMobileUrl(),
                            null,
                            null
                    ))
                    .toList();
            return new SeasonalAdminDtos.SeasonalFoodSourceOptionListResponse(items);
        }

        Fp341SeasonalTermRange activeRange = activeRanges.get(0);
        items = seasonalBaseFoodRepository.findOptions(null, activeRange.getSeasonalTerm()).stream()
                .map(food -> new SeasonalAdminDtos.SeasonalFoodSourceOption(
                        food.getId(),
                        food.getMonth(),
                        food.getSeasonalTerm(),
                        food.getCategory(),
                        food.getFoodName(),
                        food.getCardImageUrl(),
                        food.getCardImageMobileUrl(),
                        activeRange.getStartDate(),
                        activeRange.getEndDate()
                ))
                .toList();
        return new SeasonalAdminDtos.SeasonalFoodSourceOptionListResponse(items);
    }

    public SeasonalImageDtos.SeasonalSourceFoodImageResponse createSeasonalSourceNasImage(Integer sourceFoodId, String imageType, MultipartFile file) {
        Fp340SeasonalBaseFood sourceFood = findSourceFood(sourceFoodId);
        validateImageType(imageType);
        requireImageFile(file);
        String normalizedType = imageType.trim().toUpperCase();
        if (CARD_DESKTOP.equals(normalizedType) && hasText(sourceFood.getCardImageUrl())) {
            throw new AppException(ErrorCode.COMMON_CONFLICT, "Desktop image already exists. Use PATCH to replace it.");
        }
        if (CARD_MOBILE.equals(normalizedType) && hasText(sourceFood.getCardImageMobileUrl())) {
            throw new AppException(ErrorCode.COMMON_CONFLICT, "Mobile image already exists. Use PATCH to replace it.");
        }
        return replaceSeasonalSourceNasImageInternal(sourceFood, normalizedType, file);
    }

    public SeasonalImageDtos.SeasonalSourceFoodImageResponse updateSeasonalSourceNasImage(Integer sourceFoodId, String imageType, MultipartFile file) {
        Fp340SeasonalBaseFood sourceFood = findSourceFood(sourceFoodId);
        validateImageType(imageType);
        requireImageFile(file);
        return replaceSeasonalSourceNasImageInternal(sourceFood, imageType.trim().toUpperCase(), file);
    }

    public SeasonalImageDtos.SeasonalSourceFoodImageResponse deleteSeasonalSourceNasImage(Integer sourceFoodId, String imageType) {
        Fp340SeasonalBaseFood sourceFood = findSourceFood(sourceFoodId);
        validateImageType(imageType);
        String normalizedType = imageType.trim().toUpperCase();

        if (CARD_DESKTOP.equals(normalizedType)) {
            String targetUrl = sourceFood.getCardImageUrl();
            if (hasText(targetUrl) && !targetUrl.equals(sourceFood.getCardImageMobileUrl())) {
                localFileStorageService.deleteByPublicUrl(targetUrl);
            }
            sourceFood.clearDesktopCardImage();
            return new SeasonalImageDtos.SeasonalSourceFoodImageResponse(sourceFood.getId(), normalizedType, null, sourceFood.getCardImageUrl(), sourceFood.getCardImageMobileUrl());
        }

        String targetUrl = sourceFood.getCardImageMobileUrl();
        if (hasText(targetUrl) && !targetUrl.equals(sourceFood.getCardImageUrl())) {
            localFileStorageService.deleteByPublicUrl(targetUrl);
        }
        sourceFood.clearMobileCardImage();
        return new SeasonalImageDtos.SeasonalSourceFoodImageResponse(sourceFood.getId(), normalizedType, null, sourceFood.getCardImageUrl(), sourceFood.getCardImageMobileUrl());
    }

    private SeasonalImageDtos.SeasonalSourceFoodImageResponse replaceSeasonalSourceNasImageInternal(Fp340SeasonalBaseFood sourceFood, String imageType, MultipartFile file) {
        String uploadedUrl = localFileStorageService.storeSeasonalImage(file, CARD_DESKTOP.equals(imageType) ? "desktop" : "mobile");
        String currentDesktop = sourceFood.getCardImageUrl();
        String currentMobile = sourceFood.getCardImageMobileUrl();
        if (CARD_DESKTOP.equals(imageType)) {
            if (hasText(currentDesktop)) {
                localFileStorageService.deleteByPublicUrl(currentDesktop);
            }
            String nextMobile = currentMobile != null && currentMobile.equals(currentDesktop) ? uploadedUrl : currentMobile;
            sourceFood.replaceCardImages(uploadedUrl, nextMobile);
        } else {
            if (hasText(currentMobile) && !currentMobile.equals(currentDesktop)) {
                localFileStorageService.deleteByPublicUrl(currentMobile);
            }
            sourceFood.replaceCardImages(currentDesktop, uploadedUrl);
        }
        return new SeasonalImageDtos.SeasonalSourceFoodImageResponse(sourceFood.getId(), imageType, uploadedUrl, sourceFood.getCardImageUrl(), sourceFood.getCardImageMobileUrl());
    }

    private Fp340SeasonalBaseFood findSourceFood(Integer sourceFoodId) {
        return seasonalBaseFoodRepository.findById(sourceFoodId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMON_NOT_FOUND, "Seasonal source food not found."));
    }

    private void validateMonth(Integer month) {
        if (month == null || month < 1 || month > 12) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "month must be between 1 and 12.");
        }
    }

    private void validateImageType(String imageType) {
        if (imageType == null || imageType.isBlank()) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "imageType is required.");
        }
        String normalizedType = imageType.trim().toUpperCase();
        if (!CARD_DESKTOP.equals(normalizedType) && !CARD_MOBILE.equals(normalizedType)) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "imageType must be CARD_DESKTOP or CARD_MOBILE.");
        }
    }

    private void requireImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "Image file is required.");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
