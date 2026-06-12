package com.plateapp.plate_main.restaurant.service;

import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import com.plateapp.plate_main.common.s3.S3UploadService;
import com.plateapp.plate_main.restaurant.dto.RestaurantAdminDtos;
import com.plateapp.plate_main.restaurant.entity.Restaurant;
import com.plateapp.plate_main.restaurant.entity.RestaurantCategory;
import com.plateapp.plate_main.restaurant.entity.RestaurantMedia;
import com.plateapp.plate_main.restaurant.entity.RestaurantMenu;
import com.plateapp.plate_main.restaurant.repository.RestaurantCategoryRepository;
import com.plateapp.plate_main.restaurant.repository.RestaurantMediaRepository;
import com.plateapp.plate_main.restaurant.repository.RestaurantMenuRepository;
import com.plateapp.plate_main.restaurant.repository.RestaurantRepository;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RestaurantAdminService {

    private static final Logger log = LoggerFactory.getLogger(RestaurantAdminService.class);

    private static final Set<String> EXPOSURE_STATUSES = Set.of("draft", "review", "published");
    private static final String MEDIA_IMAGE = "image";
    private static final String MEDIA_VIDEO = "video";
    private static final String USAGE_REPRESENTATIVE = "representative";
    private static final String USAGE_MENU = "menu";

    private final RestaurantRepository restaurantRepository;
    private final RestaurantCategoryRepository categoryRepository;
    private final RestaurantMenuRepository menuRepository;
    private final RestaurantMediaRepository mediaRepository;
    private final S3UploadService s3UploadService;

    @Transactional(readOnly = true)
    public RestaurantAdminDtos.RestaurantListResponse listRestaurants(
            String keyword,
            String category,
            String exposureStatus,
            int page,
            int size
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));
        PageRequest pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("id"))
        );

        Page<Restaurant> result = restaurantRepository.searchAdminRestaurants(
                normalizeSearchKeyword(keyword),
                normalizeNullable(category),
                normalizeExposureStatusNullable(exposureStatus),
                pageable
        );

        List<RestaurantAdminDtos.RestaurantListItem> content = result.getContent().stream()
                .map(this::toListItem)
                .toList();

        return new RestaurantAdminDtos.RestaurantListResponse(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.hasNext()
        );
    }

    @Transactional(readOnly = true)
    public RestaurantAdminDtos.RestaurantDetailResponse getRestaurant(Long restaurantId) {
        Restaurant restaurant = findRestaurant(restaurantId);
        return toDetailResponse(restaurant);
    }

    @Transactional
    public RestaurantAdminDtos.RestaurantIdResponse createRestaurant(RestaurantAdminDtos.RestaurantUpsertRequest request) {
        ValidatedRestaurantRequest validated = validateRequest(request);
        Restaurant restaurant = restaurantRepository.save(Restaurant.create(
                validated.title(),
                validated.address(),
                validated.phone(),
                validated.businessHours(),
                validated.introduction(),
                validated.exposureStatus()
        ));

        replaceChildren(restaurant.getId(), validated);
        return new RestaurantAdminDtos.RestaurantIdResponse(restaurant.getId());
    }

    @Transactional
    public RestaurantAdminDtos.RestaurantIdResponse updateRestaurant(Long restaurantId, RestaurantAdminDtos.RestaurantUpsertRequest request) {
        Restaurant restaurant = findRestaurant(restaurantId);
        ValidatedRestaurantRequest validated = validateRequest(request);
        Set<String> existingMediaUrls = loadRestaurantMediaUrls(restaurant.getId());
        restaurant.update(
                validated.title(),
                validated.address(),
                validated.phone(),
                validated.businessHours(),
                validated.introduction(),
                validated.exposureStatus()
        );

        replaceChildren(restaurant.getId(), validated);
        deleteRemovedMediaObjects(existingMediaUrls, collectRequestedMediaUrls(validated));
        return new RestaurantAdminDtos.RestaurantIdResponse(restaurant.getId());
    }

    @Transactional
    public RestaurantAdminDtos.RestaurantDeleteResponse deleteRestaurant(Long restaurantId) {
        Restaurant restaurant = findRestaurant(restaurantId);
        Set<String> existingMediaUrls = loadRestaurantMediaUrls(restaurant.getId());
        restaurantRepository.delete(restaurant);
        deleteMediaObjects(existingMediaUrls);
        return new RestaurantAdminDtos.RestaurantDeleteResponse(true);
    }

    private void replaceChildren(Long restaurantId, ValidatedRestaurantRequest request) {
        mediaRepository.deleteByRestaurantId(restaurantId);
        menuRepository.deleteByRestaurantId(restaurantId);
        categoryRepository.deleteByRestaurantId(restaurantId);
        mediaRepository.flush();
        menuRepository.flush();
        categoryRepository.flush();

        for (int i = 0; i < request.categories().size(); i++) {
            categoryRepository.save(RestaurantCategory.create(restaurantId, request.categories().get(i), i));
        }

        for (RestaurantAdminDtos.RestaurantMediaRequest media : request.media()) {
            mediaRepository.save(toMediaEntity(restaurantId, null, media, USAGE_REPRESENTATIVE));
        }

        for (RestaurantAdminDtos.RestaurantMenuRequest menuRequest : request.menus()) {
            RestaurantMenu menu = menuRepository.save(RestaurantMenu.create(
                    restaurantId,
                    normalizeRequired(menuRequest.name(), "menus[].name is required"),
                    normalizePrice(menuRequest.price()),
                    normalizeNullable(menuRequest.description()),
                    defaultDisplayOrder(menuRequest.displayOrder())
            ));
            for (RestaurantAdminDtos.RestaurantMediaRequest media : nullToEmpty(menuRequest.media())) {
                mediaRepository.save(toMediaEntity(restaurantId, menu.getId(), media, USAGE_MENU));
            }
        }
    }

    private RestaurantMedia toMediaEntity(
            Long restaurantId,
            Long menuId,
            RestaurantAdminDtos.RestaurantMediaRequest request,
            String requiredUsageType
    ) {
        String mediaType = normalizeMediaType(request.mediaType());
        String usageType = normalizeUsageType(request.usageType());
        if (!requiredUsageType.equals(usageType)) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "Invalid media usageType.");
        }
        if (USAGE_REPRESENTATIVE.equals(usageType) && menuId != null) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "Representative media cannot have menu.");
        }
        if (USAGE_MENU.equals(usageType) && menuId == null) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "Menu media requires menu.");
        }

        Long fileSizeBytes = request.fileSizeBytes();
        if (fileSizeBytes != null && fileSizeBytes < 0) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "fileSizeBytes must be greater than or equal to 0.");
        }

        return RestaurantMedia.create(
                restaurantId,
                menuId,
                mediaType,
                usageType,
                requireMediaObjectKey(request.fileUrl()),
                normalizeNullable(request.originalName()),
                normalizeNullable(request.mimeType()),
                fileSizeBytes,
                defaultDisplayOrder(request.displayOrder())
        );
    }

    private RestaurantAdminDtos.RestaurantDetailResponse toDetailResponse(Restaurant restaurant) {
        List<String> categories = categoryRepository.findByRestaurantIdOrderByDisplayOrderAscIdAsc(restaurant.getId())
                .stream()
                .map(RestaurantCategory::getCategoryCode)
                .toList();
        List<RestaurantMedia> representativeMedia = mediaRepository.findByRestaurantIdAndMenuIdIsNullOrderByDisplayOrderAscIdAsc(restaurant.getId());
        List<RestaurantMenu> menus = menuRepository.findByRestaurantIdOrderByDisplayOrderAscIdAsc(restaurant.getId());
        List<Long> menuIds = menus.stream().map(RestaurantMenu::getId).toList();
        Map<Long, List<RestaurantMedia>> mediaByMenuId = menuIds.isEmpty()
                ? Map.of()
                : mediaRepository.findByMenuIdInOrderByDisplayOrderAscIdAsc(menuIds).stream()
                        .collect(Collectors.groupingBy(RestaurantMedia::getMenuId));

        return new RestaurantAdminDtos.RestaurantDetailResponse(
                restaurant.getId(),
                restaurant.getTitle(),
                restaurant.getAddress(),
                restaurant.getPhone(),
                restaurant.getBusinessHours(),
                restaurant.getIntroduction(),
                restaurant.getExposureStatus(),
                categories,
                representativeMedia.stream().map(this::toMediaResponse).toList(),
                menus.stream()
                        .map(menu -> toMenuResponse(menu, mediaByMenuId.getOrDefault(menu.getId(), List.of())))
                        .toList(),
                restaurant.getCreatedAt(),
                restaurant.getUpdatedAt()
        );
    }

    private RestaurantAdminDtos.RestaurantListItem toListItem(Restaurant restaurant) {
        List<String> categories = categoryRepository.findByRestaurantIdOrderByDisplayOrderAscIdAsc(restaurant.getId())
                .stream()
                .map(RestaurantCategory::getCategoryCode)
                .toList();
        String representativeImageUrl = mediaRepository.findByRestaurantIdAndMenuIdIsNullOrderByDisplayOrderAscIdAsc(restaurant.getId())
                .stream()
                .filter(media -> MEDIA_IMAGE.equals(media.getMediaType()))
                .min(Comparator.comparing(RestaurantMedia::getDisplayOrder).thenComparing(RestaurantMedia::getId))
                .map(RestaurantMedia::getFileUrl)
                .map(s3UploadService::toDeliveryUrl)
                .orElse(null);
        return new RestaurantAdminDtos.RestaurantListItem(
                restaurant.getId(),
                restaurant.getTitle(),
                restaurant.getAddress(),
                categories,
                restaurant.getExposureStatus(),
                representativeImageUrl,
                menuRepository.countByRestaurantId(restaurant.getId()),
                restaurant.getUpdatedAt()
        );
    }

    private RestaurantAdminDtos.RestaurantMenuResponse toMenuResponse(RestaurantMenu menu, List<RestaurantMedia> media) {
        return new RestaurantAdminDtos.RestaurantMenuResponse(
                menu.getId(),
                menu.getName(),
                menu.getPrice(),
                menu.getDescription(),
                menu.getDisplayOrder(),
                media.stream().map(this::toMediaResponse).toList()
        );
    }

    private RestaurantAdminDtos.RestaurantMediaResponse toMediaResponse(RestaurantMedia media) {
        return new RestaurantAdminDtos.RestaurantMediaResponse(
                media.getId(),
                media.getMediaType(),
                media.getUsageType(),
                s3UploadService.toDeliveryUrl(media.getFileUrl()),
                media.getOriginalName(),
                media.getMimeType(),
                media.getFileSizeBytes(),
                media.getDisplayOrder()
        );
    }

    private Restaurant findRestaurant(Long restaurantId) {
        if (restaurantId == null) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "restaurantId is required.");
        }
        return restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMON_NOT_FOUND, "Restaurant not found."));
    }

    private ValidatedRestaurantRequest validateRequest(RestaurantAdminDtos.RestaurantUpsertRequest request) {
        if (request == null) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "Request body is required.");
        }

        String title = normalizeRequired(request.title(), "title is required");
        if (title.length() > 150) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "title must be 150 characters or less.");
        }
        String address = normalizeRequired(request.address(), "address is required");
        if (address.length() > 300) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "address must be 300 characters or less.");
        }

        List<String> categories = nullToEmpty(request.categories()).stream()
                .map(category -> normalizeRequired(category, "categories[] is required"))
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream()
                .toList();
        if (categories.isEmpty() || categories.size() > 4) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "categories must contain 1 to 4 items.");
        }

        return new ValidatedRestaurantRequest(
                title,
                address,
                normalizeNullable(request.phone()),
                normalizeNullable(request.businessHours()),
                normalizeNullable(request.introduction()),
                normalizeExposureStatusRequired(request.exposureStatus()),
                categories,
                nullToEmpty(request.media()),
                nullToEmpty(request.menus())
        );
    }

    private String normalizeExposureStatusRequired(String value) {
        String normalized = normalizeExposureStatusNullable(value);
        if (normalized == null) {
            return "draft";
        }
        return normalized;
    }

    private String normalizeExposureStatusNullable(String value) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            return null;
        }
        normalized = normalized.toLowerCase(Locale.ROOT);
        if (!EXPOSURE_STATUSES.contains(normalized)) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "exposureStatus must be draft, review, or published.");
        }
        return normalized;
    }

    private String normalizeMediaType(String value) {
        String normalized = normalizeRequired(value, "mediaType is required").toLowerCase(Locale.ROOT);
        if (!MEDIA_IMAGE.equals(normalized) && !MEDIA_VIDEO.equals(normalized)) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "mediaType must be image or video.");
        }
        return normalized;
    }

    private String normalizeUsageType(String value) {
        String normalized = normalizeRequired(value, "usageType is required").toLowerCase(Locale.ROOT);
        if (!USAGE_REPRESENTATIVE.equals(normalized) && !USAGE_MENU.equals(normalized)) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "usageType must be representative or menu.");
        }
        return normalized;
    }

    private BigDecimal normalizePrice(BigDecimal price) {
        if (price != null && price.signum() < 0) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "menus[].price must be greater than or equal to 0.");
        }
        return price;
    }

    private Integer defaultDisplayOrder(Integer value) {
        if (value == null) {
            return 0;
        }
        if (value < 0) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "displayOrder must be greater than or equal to 0.");
        }
        return value;
    }

    private String normalizeRequired(String value, String message) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, message);
        }
        return normalized;
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeSearchKeyword(String value) {
        String normalized = normalizeNullable(value);
        return normalized == null ? "" : normalized;
    }

    private Set<String> loadRestaurantMediaUrls(Long restaurantId) {
        return mediaRepository.findByRestaurantIdOrderByDisplayOrderAscIdAsc(restaurantId).stream()
                .map(RestaurantMedia::getFileUrl)
                .map(this::toMediaObjectKey)
                .filter(url -> url != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> collectRequestedMediaUrls(ValidatedRestaurantRequest request) {
        Set<String> urls = new LinkedHashSet<>();
        for (RestaurantAdminDtos.RestaurantMediaRequest media : request.media()) {
            addMediaUrl(urls, media);
        }
        for (RestaurantAdminDtos.RestaurantMenuRequest menu : request.menus()) {
            for (RestaurantAdminDtos.RestaurantMediaRequest media : nullToEmpty(menu.media())) {
                addMediaUrl(urls, media);
            }
        }
        return urls;
    }

    private void addMediaUrl(Set<String> urls, RestaurantAdminDtos.RestaurantMediaRequest media) {
        String url = media == null ? null : normalizeNullable(media.fileUrl());
        if (url != null) {
            urls.add(requireMediaObjectKey(url));
        }
    }

    private void deleteRemovedMediaObjects(Set<String> existingMediaUrls, Set<String> requestedMediaUrls) {
        Set<String> removedMediaUrls = new LinkedHashSet<>(existingMediaUrls);
        removedMediaUrls.removeAll(requestedMediaUrls);
        deleteMediaObjects(removedMediaUrls);
    }

    private void deleteMediaObjects(Set<String> mediaUrls) {
        for (String mediaUrl : mediaUrls) {
            try {
                s3UploadService.deleteObjectByKey(mediaUrl);
            } catch (RuntimeException e) {
                log.warn("Failed to delete restaurant media from S3. objectKey={}", mediaUrl, e);
            }
        }
    }

    private String requireMediaObjectKey(String fileUrl) {
        String normalized = normalizeRequired(fileUrl, "media[].fileUrl is required");
        String objectKey = normalizeNullable(s3UploadService.toObjectKey(normalized));
        if (objectKey == null) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "media[].fileUrl must be a valid S3 or CDN URL.");
        }
        return objectKey;
    }

    private String toMediaObjectKey(String fileUrl) {
        String normalized = normalizeNullable(fileUrl);
        if (normalized == null) {
            return null;
        }
        String objectKey = normalizeNullable(s3UploadService.toObjectKey(normalized));
        return objectKey == null ? normalized : objectKey;
    }

    private <T> List<T> nullToEmpty(List<T> values) {
        return values == null ? List.of() : values;
    }

    private record ValidatedRestaurantRequest(
            String title,
            String address,
            String phone,
            String businessHours,
            String introduction,
            String exposureStatus,
            List<String> categories,
            List<RestaurantAdminDtos.RestaurantMediaRequest> media,
            List<RestaurantAdminDtos.RestaurantMenuRequest> menus
    ) {
    }
}
