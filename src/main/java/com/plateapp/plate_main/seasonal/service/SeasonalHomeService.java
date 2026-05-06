package com.plateapp.plate_main.seasonal.service;

import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import com.plateapp.plate_main.seasonal.base.Fp340SeasonalBaseFood;
import com.plateapp.plate_main.seasonal.base.Fp341SeasonalTermRange;
import com.plateapp.plate_main.seasonal.base.repository.Fp340SeasonalBaseFoodRepository;
import com.plateapp.plate_main.seasonal.base.repository.Fp341SeasonalTermRangeRepository;
import com.plateapp.plate_main.seasonal.dto.SeasonalHomeDtos;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SeasonalHomeService {

    private final Fp340SeasonalBaseFoodRepository seasonalBaseFoodRepository;
    private final Fp341SeasonalTermRangeRepository seasonalTermRangeRepository;

    public SeasonalHomeService(
            Fp340SeasonalBaseFoodRepository seasonalBaseFoodRepository,
            Fp341SeasonalTermRangeRepository seasonalTermRangeRepository
    ) {
        this.seasonalBaseFoodRepository = seasonalBaseFoodRepository;
        this.seasonalTermRangeRepository = seasonalTermRangeRepository;
    }

    public SeasonalHomeDtos.SeasonalHomeResponse getSeasonalHome(
            String basis,
            Integer month,
            LocalDate date,
            Integer selectedFoodId,
            Boolean isGuest,
            String guestId
    ) {
        SeasonalHomeDtos.Basis resolvedBasis = resolveBasis(basis);
        LocalDate referenceDate = date != null ? date : LocalDate.now();
        Integer targetMonth = resolvedBasis == SeasonalHomeDtos.Basis.MONTH
                ? (month != null ? validateMonth(month) : referenceDate.getMonthValue())
                : null;
        String seasonalTerm = resolvedBasis == SeasonalHomeDtos.Basis.TERM
                ? resolveSeasonalTerm(referenceDate)
                : null;

        List<Fp340SeasonalBaseFood> foods = loadFoods(resolvedBasis, targetMonth, seasonalTerm, referenceDate);
        if (foods.isEmpty()) {
            return emptyResponse(resolvedBasis, referenceDate, targetMonth, seasonalTerm);
        }

        Fp340SeasonalBaseFood heroFood = resolveHeroFood(foods, selectedFoodId);
        return buildResponse(resolvedBasis, referenceDate, targetMonth, seasonalTerm, foods, heroFood);
    }

    public SeasonalHomeDtos.SeasonalHomeResponse getSeasonalHomeByFoodId(
            Integer foodId,
            String basis,
            Integer month,
            LocalDate date,
            Boolean isGuest,
            String guestId
    ) {
        SeasonalHomeDtos.Basis resolvedBasis = resolveBasis(basis);
        LocalDate referenceDate = date != null ? date : LocalDate.now();
        Integer targetMonth = resolvedBasis == SeasonalHomeDtos.Basis.MONTH
                ? (month != null ? validateMonth(month) : referenceDate.getMonthValue())
                : null;
        String seasonalTerm = resolvedBasis == SeasonalHomeDtos.Basis.TERM
                ? resolveSeasonalTerm(referenceDate)
                : null;

        List<Fp340SeasonalBaseFood> foods = loadFoods(resolvedBasis, targetMonth, seasonalTerm, referenceDate);
        if (foods.isEmpty()) {
            return emptyResponse(resolvedBasis, referenceDate, targetMonth, seasonalTerm);
        }

        Fp340SeasonalBaseFood heroFood = foods.stream()
                .filter(food -> food.getId().equals(foodId))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.COMMON_INVALID_INPUT, "Selected seasonal food is not in the current scope."));

        return buildResponse(resolvedBasis, referenceDate, targetMonth, seasonalTerm, foods, heroFood);
    }

    private List<Fp340SeasonalBaseFood> loadFoods(
            SeasonalHomeDtos.Basis basis,
            Integer targetMonth,
            String seasonalTerm,
            LocalDate referenceDate
    ) {
        List<Fp340SeasonalBaseFood> foods;
        if (basis == SeasonalHomeDtos.Basis.MONTH) {
            foods = seasonalBaseFoodRepository.findOptions(targetMonth, null);
        } else {
            if (seasonalTerm == null || seasonalTerm.isBlank()) {
                return List.of();
            }
            foods = seasonalBaseFoodRepository.findOptions(null, seasonalTerm);
        }

        List<Fp340SeasonalBaseFood> sorted = new ArrayList<>(foods);
        sorted.sort(Comparator
                .comparing(Fp340SeasonalBaseFood::getCategory, Comparator.nullsLast(String::compareTo))
                .thenComparing(Fp340SeasonalBaseFood::getFoodName, Comparator.nullsLast(String::compareTo))
                .thenComparing(Fp340SeasonalBaseFood::getId, Comparator.nullsLast(Integer::compareTo))
        );
        return sorted;
    }

    private SeasonalHomeDtos.SeasonalHomeResponse buildResponse(
            SeasonalHomeDtos.Basis basis,
            LocalDate referenceDate,
            Integer targetMonth,
            String seasonalTerm,
            List<Fp340SeasonalBaseFood> foods,
            Fp340SeasonalBaseFood heroFood
    ) {
        return new SeasonalHomeDtos.SeasonalHomeResponse(
                new SeasonalHomeDtos.BasisInfo(
                        basis.name(),
                        referenceDate.toString(),
                        targetMonth,
                        seasonalTerm
                ),
                toHero(heroFood),
                foods.stream()
                        .map(food -> new SeasonalHomeDtos.Chip(
                                food.getId(),
                                food.getFoodName(),
                                food.getId().equals(heroFood.getId())
                        ))
                        .toList(),
                foods.stream().map(this::toFoodItem).toList()
        );
    }

    private SeasonalHomeDtos.SeasonalHomeResponse emptyResponse(
            SeasonalHomeDtos.Basis basis,
            LocalDate referenceDate,
            Integer targetMonth,
            String seasonalTerm
    ) {
        return new SeasonalHomeDtos.SeasonalHomeResponse(
                new SeasonalHomeDtos.BasisInfo(
                        basis.name(),
                        referenceDate.toString(),
                        targetMonth,
                        seasonalTerm
                ),
                null,
                List.of(),
                List.of()
        );
    }

    private SeasonalHomeDtos.Hero toHero(Fp340SeasonalBaseFood food) {
        return new SeasonalHomeDtos.Hero(
                food.getId(),
                food.getSeasonalTerm(),
                food.getMonth(),
                food.getMonth() + " Seasonal Picks",
                food.getFoodName(),
                food.getCategory(),
                food.getMonth() + " month seasonal highlight",
                "Explore the seasonal flow around " + food.getFoodName() + ".",
                food.getCardImageUrl(),
                food.getCardImageMobileUrl()
        );
    }

    private SeasonalHomeDtos.FoodItem toFoodItem(Fp340SeasonalBaseFood food) {
        return new SeasonalHomeDtos.FoodItem(
                food.getId(),
                food.getSeasonalTerm(),
                food.getMonth(),
                food.getFoodName(),
                food.getCategory(),
                food.getCardImageUrl(),
                food.getCardImageMobileUrl()
        );
    }

    private Fp340SeasonalBaseFood resolveHeroFood(List<Fp340SeasonalBaseFood> foods, Integer selectedFoodId) {
        if (selectedFoodId == null) {
            return foods.get(0);
        }
        return foods.stream()
                .filter(food -> food.getId().equals(selectedFoodId))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.COMMON_INVALID_INPUT, "Selected seasonal food is not in the current scope."));
    }

    private SeasonalHomeDtos.Basis resolveBasis(String basis) {
        if (basis == null || basis.isBlank()) {
            return SeasonalHomeDtos.Basis.MONTH;
        }
        try {
            return SeasonalHomeDtos.Basis.valueOf(basis.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "basis must be MONTH or TERM.");
        }
    }

    private Integer validateMonth(Integer month) {
        if (month == null || month < 1 || month > 12) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "month must be between 1 and 12.");
        }
        return month;
    }

    private String resolveSeasonalTerm(LocalDate referenceDate) {
        return seasonalTermRangeRepository.findActiveRanges(referenceDate).stream()
                .findFirst()
                .map(Fp341SeasonalTermRange::getSeasonalTerm)
                .orElse(null);
    }
}