package com.plateapp.plate_main.video.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plateapp.plate_main.feed.entity.Fp400ImageFeed;
import com.plateapp.plate_main.feed.repository.ImageFeedRepository;
import com.plateapp.plate_main.video.entity.Fp300Store;
import com.plateapp.plate_main.video.entity.Fp310Place;
import com.plateapp.plate_main.video.repository.Fp300StoreRepository;
import com.plateapp.plate_main.video.repository.Fp310PlaceRepository;

@Service
public class ContentPlaceResolver {

    private static final String FLAG_Y = "Y";

    private final Fp310PlaceRepository fp310PlaceRepository;
    private final Fp300StoreRepository fp300StoreRepository;
    private final ImageFeedRepository imageFeedRepository;

    public ContentPlaceResolver(
            Fp310PlaceRepository fp310PlaceRepository,
            Fp300StoreRepository fp300StoreRepository,
            ImageFeedRepository imageFeedRepository
    ) {
        this.fp310PlaceRepository = fp310PlaceRepository;
        this.fp300StoreRepository = fp300StoreRepository;
        this.imageFeedRepository = imageFeedRepository;
    }

    @Transactional(readOnly = true)
    public ResolvedPlace resolve(String placeId, String storeName, String address) {
        String safePlaceId = trimToNull(placeId);
        String safeStoreName = trimToNull(storeName);
        String safeAddress = trimToNull(address);

        Optional<Fp310Place> directPlace = findPlaceById(safePlaceId);
        if (directPlace.isPresent()) {
            return toResolvedPlace(directPlace.get(), safeAddress);
        }

        String inferredPlaceId = inferPlaceId(safeStoreName, safeAddress);
        Optional<Fp310Place> inferredPlace = findPlaceById(inferredPlaceId);
        if (inferredPlace.isPresent()) {
            return toResolvedPlace(inferredPlace.get(), safeAddress);
        }

        Optional<Fp310Place> addressPlace = findPlaceByAddress(safeAddress);
        if (addressPlace.isPresent()) {
            return toResolvedPlace(addressPlace.get(), safeAddress);
        }

        return new ResolvedPlace(
                firstNonBlank(safePlaceId, inferredPlaceId),
                null,
                null,
                safeAddress
        );
    }

    /**
     * Resolve only the content's explicit canonical place link. This is used by
     * ID-based detail contracts where name/address inference could attach the
     * content to a different legacy place.
     */
    @Transactional(readOnly = true)
    public ResolvedPlace resolveDirect(String placeId, String address) {
        String safePlaceId = trimToNull(placeId);
        String safeAddress = trimToNull(address);
        Optional<Fp310Place> directPlace = findPlaceById(safePlaceId);
        if (directPlace.isPresent()) {
            return toResolvedPlace(directPlace.get(), safeAddress);
        }
        return new ResolvedPlace(safePlaceId, null, null, safeAddress);
    }

    private Optional<Fp310Place> findPlaceById(String placeId) {
        if (placeId == null) {
            return Optional.empty();
        }
        return fp310PlaceRepository.findByPlaceIdAndUseYnAndDeletedAtIsNull(placeId, FLAG_Y)
                .filter(place -> place.getLatitude() != null && place.getLongitude() != null);
    }

    private String inferPlaceId(String storeName, String address) {
        if (storeName != null && address != null) {
            Optional<Fp300Store> store = fp300StoreRepository
                    .findTopByUseYnAndOpenYnAndDeletedAtIsNullAndFileNameIsNotNullAndPlaceIdIsNotNullAndStoreNameAndAddressOrderByCreatedAtDescStoreIdDesc(
                            FLAG_Y,
                            FLAG_Y,
                            storeName,
                            address
                    );
            if (store.isPresent()) {
                return trimToNull(store.get().getPlaceId());
            }

            Optional<Fp400ImageFeed> feed = imageFeedRepository
                    .findTopByUseYnAndPlaceIdIsNotNullAndStoreNameAndLocationOrderByCreatedAtDescFeedIdDesc(
                            FLAG_Y,
                            storeName,
                            address
                    );
            if (feed.isPresent()) {
                return trimToNull(feed.get().getPlaceId());
            }
        }
        return null;
    }

    private Optional<Fp310Place> findPlaceByAddress(String address) {
        String normalizedAddress = normalizeAddressPrefix(address);
        if (normalizedAddress == null) {
            return Optional.empty();
        }
        return fp310PlaceRepository
                .findTopByFormattedAddressStartingWithAndUseYnAndDeletedAtIsNullAndLatitudeIsNotNullAndLongitudeIsNotNullOrderByIdDesc(
                        normalizedAddress,
                        FLAG_Y
                );
    }

    private ResolvedPlace toResolvedPlace(Fp310Place place, String fallbackAddress) {
        return new ResolvedPlace(
                trimToNull(place.getPlaceId()),
                place.getLatitude(),
                place.getLongitude(),
                firstNonBlank(trimToNull(place.getFormattedAddress()), fallbackAddress)
        );
    }

    private String normalizeAddressPrefix(String address) {
        String value = trimToNull(address);
        if (value == null) {
            return null;
        }

        int commaIndex = value.indexOf(',');
        if (commaIndex >= 0) {
            value = value.substring(0, commaIndex).trim();
        }

        value = value.replaceAll("\\s+\\d+층.*$", "");
        value = value.replaceAll("\\s+\\d+호.*$", "");
        value = value.replaceAll("\\s+", " ").trim();

        return value.isBlank() ? null : value;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String firstNonBlank(String first, String second) {
        return trimToNull(first) != null ? trimToNull(first) : trimToNull(second);
    }

    public record ResolvedPlace(
            String placeId,
            Double lat,
            Double lng,
            String address
    ) {
    }
}
