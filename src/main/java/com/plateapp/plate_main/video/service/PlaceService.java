package com.plateapp.plate_main.video.service;

import com.plateapp.plate_main.video.entity.Fp310Place;
import com.plateapp.plate_main.video.repository.Fp310PlaceRepository;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Value;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlaceService {

    private final Fp310PlaceRepository repository;

    @Transactional
    public void savePlace(PlaceRequest req) {
        Fp310Place place = repository.findByPlaceIdAndUseYnAndDeletedAtIsNull(req.getPlaceId(), "Y")
                .orElseGet(Fp310Place::new);

        place.setPlaceId(req.getPlaceId());
        place.setFormattedAddress(req.getAddress());
        place.setLatitude(req.getLat());
        place.setLongitude(req.getLng());
        place.setUseYn("Y");

        repository.save(place);
    }

    @Value
    @Builder
    public static class PlaceRequest {
        @NotBlank
        String placeId;
        @NotBlank
        String address;
        Double lat;
        Double lng;
    }
}
