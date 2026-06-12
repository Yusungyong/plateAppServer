package com.plateapp.plate_main.restaurant.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.plateapp.plate_main.common.s3.S3UploadService;
import com.plateapp.plate_main.restaurant.dto.RestaurantAdminDtos;
import com.plateapp.plate_main.restaurant.entity.Restaurant;
import com.plateapp.plate_main.restaurant.entity.RestaurantCategory;
import com.plateapp.plate_main.restaurant.entity.RestaurantMedia;
import com.plateapp.plate_main.restaurant.repository.RestaurantCategoryRepository;
import com.plateapp.plate_main.restaurant.repository.RestaurantMediaRepository;
import com.plateapp.plate_main.restaurant.repository.RestaurantMenuRepository;
import com.plateapp.plate_main.restaurant.repository.RestaurantRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RestaurantAdminServiceTest {

    private static final String OLD_URL = "https://foodplayerbucket.s3.ap-northeast-2.amazonaws.com/restaurants/old.jpg";
    private static final String KEEP_URL = "https://foodplayerbucket.s3.ap-northeast-2.amazonaws.com/restaurants/keep.jpg";
    private static final String NEW_URL = "https://foodplayerbucket.s3.ap-northeast-2.amazonaws.com/restaurants/new.jpg";
    private static final String OLD_KEY = "restaurants/old.jpg";
    private static final String KEEP_KEY = "restaurants/keep.jpg";
    private static final String NEW_KEY = "restaurants/new.jpg";

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private RestaurantCategoryRepository categoryRepository;

    @Mock
    private RestaurantMenuRepository menuRepository;

    @Mock
    private RestaurantMediaRepository mediaRepository;

    @Mock
    private S3UploadService s3UploadService;

    private RestaurantAdminService service;

    @BeforeEach
    void setUp() {
        service = new RestaurantAdminService(
                restaurantRepository,
                categoryRepository,
                menuRepository,
                mediaRepository,
                s3UploadService
        );
    }

    @Test
    void updateRestaurantDeletesOnlyRemovedMediaObjects() {
        Restaurant restaurant = restaurant(1L);
        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));
        when(mediaRepository.findByRestaurantIdOrderByDisplayOrderAscIdAsc(1L))
                .thenReturn(List.of(media(OLD_URL), media(KEEP_URL)));
        when(categoryRepository.save(any(RestaurantCategory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mediaRepository.save(any(RestaurantMedia.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(s3UploadService.toObjectKey(OLD_URL)).thenReturn(OLD_KEY);
        when(s3UploadService.toObjectKey(KEEP_URL)).thenReturn(KEEP_KEY);
        when(s3UploadService.toObjectKey(NEW_URL)).thenReturn(NEW_KEY);

        service.updateRestaurant(1L, upsertRequest(List.of(mediaRequest(KEEP_URL, 0), mediaRequest(NEW_URL, 1))));

        verify(s3UploadService).deleteObjectByKey(OLD_KEY);
        verify(s3UploadService, never()).deleteObjectByKey(KEEP_KEY);
        verify(s3UploadService, never()).deleteObjectByKey(NEW_KEY);
    }

    @Test
    void deleteRestaurantDeletesAllExistingMediaObjects() {
        Restaurant restaurant = restaurant(1L);
        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));
        when(mediaRepository.findByRestaurantIdOrderByDisplayOrderAscIdAsc(1L))
                .thenReturn(List.of(media(OLD_URL), media(KEEP_URL)));
        when(s3UploadService.toObjectKey(OLD_URL)).thenReturn(OLD_KEY);
        when(s3UploadService.toObjectKey(KEEP_URL)).thenReturn(KEEP_KEY);

        service.deleteRestaurant(1L);

        verify(restaurantRepository).delete(restaurant);
        verify(s3UploadService).deleteObjectByKey(OLD_KEY);
        verify(s3UploadService).deleteObjectByKey(KEEP_KEY);
    }

    @Test
    void updateRestaurantDoesNotDeleteMediaWhenExistingUrlIsReused() {
        Restaurant restaurant = restaurant(1L);
        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));
        when(mediaRepository.findByRestaurantIdOrderByDisplayOrderAscIdAsc(1L))
                .thenReturn(List.of(media(KEEP_URL)));
        when(categoryRepository.save(any(RestaurantCategory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mediaRepository.save(any(RestaurantMedia.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(s3UploadService.toObjectKey(KEEP_URL)).thenReturn(KEEP_KEY);

        service.updateRestaurant(1L, upsertRequest(List.of(mediaRequest(KEEP_URL, 0))));

        verify(mediaRepository).save(argThat(media -> KEEP_KEY.equals(media.getFileUrl())));
        verify(s3UploadService, never()).deleteObjectByKey(KEEP_KEY);
    }

    private Restaurant restaurant(Long id) {
        Restaurant restaurant = Restaurant.create(
                "Plating Kitchen",
                "123 Teheran-ro, Gangnam-gu, Seoul",
                "02-1234-5678",
                "Every day 11:00 - 22:00",
                "Intro",
                "draft"
        );
        ReflectionTestUtils.setField(restaurant, "id", id);
        return restaurant;
    }

    private RestaurantMedia media(String fileUrl) {
        return RestaurantMedia.create(
                1L,
                null,
                "image",
                "representative",
                fileUrl,
                "image.jpg",
                "image/jpeg",
                123L,
                0
        );
    }

    private RestaurantAdminDtos.RestaurantUpsertRequest upsertRequest(
            List<RestaurantAdminDtos.RestaurantMediaRequest> media
    ) {
        return new RestaurantAdminDtos.RestaurantUpsertRequest(
                "Plating Kitchen",
                "123 Teheran-ro, Gangnam-gu, Seoul",
                "02-1234-5678",
                "Every day 11:00 - 22:00",
                "Intro",
                "draft",
                List.of("KOREAN"),
                media,
                List.of()
        );
    }

    private RestaurantAdminDtos.RestaurantMediaRequest mediaRequest(String fileUrl, int displayOrder) {
        return new RestaurantAdminDtos.RestaurantMediaRequest(
                "image",
                "representative",
                fileUrl,
                "image.jpg",
                "image/jpeg",
                123L,
                displayOrder
        );
    }
}
