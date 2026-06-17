package com.plateapp.plate_main.owner.service;

import com.plateapp.plate_main.auth.domain.User;
import com.plateapp.plate_main.auth.repository.UserRepository;
import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import com.plateapp.plate_main.common.s3.S3UploadService;
import com.plateapp.plate_main.owner.entity.StoreOwner;
import com.plateapp.plate_main.owner.repository.StoreOwnerRepository;
import com.plateapp.plate_main.restaurant.dto.RestaurantAdminDtos;
import com.plateapp.plate_main.restaurant.entity.Restaurant;
import com.plateapp.plate_main.restaurant.entity.RestaurantCategory;
import com.plateapp.plate_main.restaurant.repository.RestaurantCategoryRepository;
import com.plateapp.plate_main.restaurant.repository.RestaurantMediaRepository;
import com.plateapp.plate_main.restaurant.repository.RestaurantMenuRepository;
import com.plateapp.plate_main.restaurant.repository.RestaurantRepository;
import com.plateapp.plate_main.restaurant.service.RestaurantAdminService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OwnerStoreService {

    private final UserRepository userRepository;
    private final StoreOwnerRepository storeOwnerRepository;
    private final RestaurantRepository restaurantRepository;
    private final RestaurantCategoryRepository categoryRepository;
    private final RestaurantMenuRepository menuRepository;
    private final RestaurantMediaRepository mediaRepository;
    private final RestaurantAdminService restaurantAdminService;
    private final S3UploadService s3UploadService;

    @Transactional(readOnly = true)
    public RestaurantAdminDtos.RestaurantListResponse list(String username, int page, int size) {
        Integer userId = currentUserId(username);
        List<RestaurantAdminDtos.RestaurantListItem> allStores = storeOwnerRepository
                .findByUserIdAndRevokedAtIsNullOrderByCreatedAtDescIdDesc(userId).stream()
                .map(StoreOwner::getStoreId)
                .distinct()
                .map(restaurantRepository::findById)
                .flatMap(java.util.Optional::stream)
                .map(this::toListItem)
                .toList();

        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));
        int from = Math.min(safePage * safeSize, allStores.size());
        int to = Math.min(from + safeSize, allStores.size());
        int totalPages = allStores.isEmpty() ? 0 : (int) Math.ceil((double) allStores.size() / safeSize);
        return new RestaurantAdminDtos.RestaurantListResponse(
                allStores.subList(from, to),
                safePage,
                safeSize,
                allStores.size(),
                totalPages,
                to < allStores.size()
        );
    }

    @Transactional(readOnly = true)
    public RestaurantAdminDtos.RestaurantDetailResponse detail(String username, Long storeId) {
        assertOwnsStore(username, storeId);
        return restaurantAdminService.getRestaurant(storeId);
    }

    @Transactional
    public RestaurantAdminDtos.RestaurantIdResponse update(
            String username,
            Long storeId,
            RestaurantAdminDtos.RestaurantUpsertRequest request
    ) {
        assertOwnsStore(username, storeId);
        return restaurantAdminService.updateRestaurant(storeId, request);
    }

    private RestaurantAdminDtos.RestaurantListItem toListItem(Restaurant restaurant) {
        List<String> categories = categoryRepository.findByRestaurantIdOrderByDisplayOrderAscIdAsc(restaurant.getId())
                .stream()
                .map(RestaurantCategory::getCategoryCode)
                .toList();
        String representativeImageUrl = mediaRepository
                .findByRestaurantIdAndMenuIdIsNullOrderByDisplayOrderAscIdAsc(restaurant.getId())
                .stream()
                .filter(media -> "image".equals(media.getMediaType()))
                .findFirst()
                .map(media -> s3UploadService.toDeliveryUrl(media.getFileUrl()))
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

    private void assertOwnsStore(String username, Long storeId) {
        Integer userId = currentUserId(username);
        if (!storeOwnerRepository.existsByStoreIdAndUserIdAndRevokedAtIsNull(storeId, userId)) {
            throw new AppException(ErrorCode.COMMON_NOT_FOUND);
        }
    }

    private Integer currentUserId(String username) {
        Integer userId = userRepository.findUserIdByUsername(username);
        if (userId == null) {
            User user = userRepository.findById(username)
                    .orElseThrow(() -> new AppException(ErrorCode.AUTH_UNAUTHORIZED));
            userId = user.getUserId();
        }
        if (userId == null) {
            throw new AppException(ErrorCode.COMMON_INTERNAL_ERROR, "사용자 식별자를 확인할 수 없습니다.");
        }
        return userId;
    }
}
