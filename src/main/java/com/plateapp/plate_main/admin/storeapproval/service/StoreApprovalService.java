package com.plateapp.plate_main.admin.storeapproval.service;

import com.plateapp.plate_main.admin.audit.service.AdminAuditService;
import com.plateapp.plate_main.admin.outbox.entity.AdminOutboxEvent;
import com.plateapp.plate_main.admin.outbox.repository.AdminOutboxEventRepository;
import com.plateapp.plate_main.admin.security.AdminActor;
import com.plateapp.plate_main.admin.storeapproval.dto.StoreApprovalDtos;
import com.plateapp.plate_main.admin.storeapproval.entity.StoreApplication;
import com.plateapp.plate_main.admin.storeapproval.entity.StoreApplicationCategory;
import com.plateapp.plate_main.admin.storeapproval.entity.StoreApplicationDocument;
import com.plateapp.plate_main.admin.storeapproval.entity.StoreApplicationMenu;
import com.plateapp.plate_main.admin.storeapproval.entity.StoreApplicationReview;
import com.plateapp.plate_main.admin.storeapproval.repository.StoreApplicationCategoryRepository;
import com.plateapp.plate_main.admin.storeapproval.repository.StoreApplicationDocumentRepository;
import com.plateapp.plate_main.admin.storeapproval.repository.StoreApplicationMenuRepository;
import com.plateapp.plate_main.admin.storeapproval.repository.StoreApplicationRepository;
import com.plateapp.plate_main.admin.storeapproval.repository.StoreApplicationReviewRepository;
import com.plateapp.plate_main.admin.storeoperation.entity.AdminStoreOperation;
import com.plateapp.plate_main.admin.storeoperation.repository.AdminStoreOperationRepository;
import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import com.plateapp.plate_main.common.filter.RequestIdFilter;
import com.plateapp.plate_main.common.s3.S3UploadService;
import com.plateapp.plate_main.owner.entity.StoreOwner;
import com.plateapp.plate_main.owner.repository.StoreOwnerRepository;
import com.plateapp.plate_main.restaurant.entity.Restaurant;
import com.plateapp.plate_main.restaurant.entity.RestaurantCategory;
import com.plateapp.plate_main.restaurant.entity.RestaurantMenu;
import com.plateapp.plate_main.restaurant.repository.RestaurantCategoryRepository;
import com.plateapp.plate_main.restaurant.repository.RestaurantMenuRepository;
import com.plateapp.plate_main.restaurant.repository.RestaurantRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StoreApprovalService {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final Set<String> APPROVAL_STATUSES = Set.of("draft", "pending", "on_hold", "approved", "rejected");
    private static final Set<String> VERIFICATION_STATUSES = Set.of("not_requested", "reviewing", "verified", "rejected");
    private static final Set<String> REJECTION_CODES = Set.of(
            "MISSING_DOCUMENT",
            "INVALID_DOCUMENT",
            "BUSINESS_INFO_MISMATCH",
            "DUPLICATE_STORE",
            "UNSUPPORTED_BUSINESS",
            "OTHER"
    );
    private static final Map<String, String> REGION_NAMES = Map.of(
            "SEOUL", "서울",
            "BUSAN", "부산",
            "DAEGU", "대구",
            "INCHEON", "인천",
            "GWANGJU", "광주",
            "DAEJEON", "대전",
            "ULSAN", "울산",
            "SEJONG", "세종"
    );
    private static final Map<String, String> CATEGORY_NAMES = Map.of(
            "KOREAN", "한식",
            "CHINESE", "중식",
            "JAPANESE", "일식",
            "WESTERN", "양식",
            "CAFE", "카페",
            "DESSERT", "디저트"
    );

    private final StoreApplicationRepository applicationRepository;
    private final StoreApplicationCategoryRepository applicationCategoryRepository;
    private final StoreApplicationMenuRepository applicationMenuRepository;
    private final StoreApplicationDocumentRepository applicationDocumentRepository;
    private final StoreApplicationReviewRepository reviewRepository;
    private final RestaurantRepository restaurantRepository;
    private final RestaurantCategoryRepository restaurantCategoryRepository;
    private final RestaurantMenuRepository restaurantMenuRepository;
    private final AdminStoreOperationRepository storeOperationRepository;
    private final AdminOutboxEventRepository outboxRepository;
    private final AdminAuditService auditService;
    private final BusinessNumberCrypto businessNumberCrypto;
    private final S3UploadService s3UploadService;
    private final StoreDocumentAccessService documentAccessService;
    private final StoreOwnerRepository storeOwnerRepository;

    @Transactional(readOnly = true)
    public StoreApprovalDtos.ListResponse list(
            int page,
            int size,
            String keyword,
            String region,
            String category,
            String status,
            String verificationStatus,
            LocalDate appliedFrom,
            LocalDate appliedTo,
            String sort
    ) {
        validatePage(page, size);
        if (appliedFrom != null && appliedTo != null && appliedFrom.isAfter(appliedTo)) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "appliedFrom must be on or before appliedTo.");
        }

        String normalizedKeyword = normalizeNullable(keyword);
        boolean hasKeyword = normalizedKeyword != null;
        String keywordPattern = hasKeyword
                ? "%" + normalizedKeyword.toLowerCase(Locale.ROOT) + "%"
                : "";
        String businessNumberHash = hasKeyword
                ? businessNumberCrypto.hash(normalizedKeyword)
                : "";
        OffsetDateTime appliedFromStart = startOfDay(appliedFrom);
        OffsetDateTime appliedToExclusive = startOfNextDay(appliedTo);
        Page<StoreApplication> result = applicationRepository.search(
                hasKeyword,
                keywordPattern,
                businessNumberHash,
                normalizeUpper(region),
                normalizeUpper(category),
                normalizeEnum(status, APPROVAL_STATUSES, "status"),
                normalizeEnum(verificationStatus, VERIFICATION_STATUSES, "verificationStatus"),
                appliedFromStart != null,
                appliedFromStart,
                appliedToExclusive != null,
                appliedToExclusive,
                PageRequest.of(page, size, parseSort(sort))
        );

        List<StoreApprovalDtos.ListItem> content = result.getContent().stream()
                .map(application -> new StoreApprovalDtos.ListItem(
                        application.getId(),
                        application.getStoreName(),
                        categories(application.getId()),
                        codeName(application.getRegionCode(), REGION_NAMES),
                        application.getAddress(),
                        application.getOwnerName(),
                        application.getApprovalStatus(),
                        application.getVerificationStatus(),
                        application.getAppliedAt(),
                        application.getUpdatedAt()
                ))
                .toList();

        return new StoreApprovalDtos.ListResponse(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.hasNext()
        );
    }

    @Transactional
    public StoreApprovalDtos.DetailResponse detail(
            Long applicationId,
            AdminActor actor,
            HttpServletRequest request
    ) {
        StoreApplication application = findApplication(applicationId);
        List<StoreApplicationDocument> documents =
                applicationDocumentRepository.findByApplicationIdOrderByCreatedAtAscIdAsc(applicationId);
        StoreApplicationReview latestReview = reviewRepository
                .findFirstByApplicationIdOrderByReviewedAtDescIdDesc(applicationId)
                .orElse(null);

        auditService.record(
                actor,
                "STORE_APPROVAL_VIEWED",
                "STORE_APPROVAL",
                applicationId,
                null,
                null,
                null,
                null,
                request
        );

        return new StoreApprovalDtos.DetailResponse(
                application.getId(),
                application.getStoreName(),
                categories(applicationId),
                codeName(application.getRegionCode(), REGION_NAMES),
                application.getAddress(),
                application.getPhone(),
                application.getEmail(),
                application.getOwnerName(),
                businessNumberCrypto.maskEncrypted(application.getBusinessNumberEncrypted()),
                application.getApprovalStatus(),
                application.getVerificationStatus(),
                s3UploadService.toDeliveryUrl(application.getMainImageObjectKey()),
                application.getDescription(),
                applicationMenuRepository.findByApplicationIdOrderByDisplayOrderAscIdAsc(applicationId).stream()
                        .map(menu -> new StoreApprovalDtos.MenuItem(
                                menu.getId(),
                                menu.getName(),
                                menu.getPrice(),
                                menu.getDescription()
                        ))
                        .toList(),
                documents.stream()
                        .map(document -> new StoreApprovalDtos.DocumentItem(
                                document.getId(),
                                document.getDocumentType(),
                                document.getOriginalName(),
                                document.getVerificationStatus()
                        ))
                        .toList(),
                application.getAppliedAt(),
                application.getUpdatedAt(),
                latestReview == null ? null : latestReview.getReason(),
                application.getStoreId(),
                application.getVersion()
        );
    }

    @Transactional
    public StoreApprovalDtos.ActionResponse approve(
            Long applicationId,
            StoreApprovalDtos.ApproveRequest command,
            AdminActor actor,
            HttpServletRequest request
    ) {
        StoreApplication application = findApplication(applicationId);
        assertVersion(application, command.version());
        assertTransitionAllowed(application, Set.of(
                StoreApplication.STATUS_PENDING,
                StoreApplication.STATUS_ON_HOLD,
                StoreApplication.STATUS_REJECTED
        ));
        if (!StoreApplication.VERIFICATION_VERIFIED.equals(application.getVerificationStatus())) {
            throw new AppException(ErrorCode.STORE_APPROVAL_VERIFICATION_INCOMPLETE);
        }
        assertDocumentsComplete(applicationId);
        if (applicationRepository.existsByBusinessNumberHashAndApprovalStatusAndIdNot(
                application.getBusinessNumberHash(),
                StoreApplication.STATUS_APPROVED,
                applicationId
        )) {
            throw new AppException(ErrorCode.STORE_APPROVAL_DUPLICATE_STORE);
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        String previousStatus = application.getApprovalStatus();
        Restaurant restaurant = activateApprovedStore(application, actor.userId());
        application.approve(restaurant.getId(), actor.userId(), now);
        application = applicationRepository.saveAndFlush(application);

        saveReview(applicationId, previousStatus, application.getApprovalStatus(), null, null,
                normalizeNullable(command.comment()), actor.userId(), now);
        recordTransitionAudit(application, previousStatus, command.comment(), null, actor, request);
        saveOutbox(application, "STORE_APPROVED", actor, now);
        return actionResponse(application);
    }

    @Transactional
    public StoreApprovalDtos.ActionResponse hold(
            Long applicationId,
            StoreApprovalDtos.HoldRequest command,
            AdminActor actor,
            HttpServletRequest request
    ) {
        String reason = validateReason(command.reason());
        StoreApplication application = findApplication(applicationId);
        assertVersion(application, command.version());
        assertTransitionAllowed(application, Set.of(StoreApplication.STATUS_PENDING));

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        String previousStatus = application.getApprovalStatus();
        application.hold(actor.userId(), now);
        application = applicationRepository.saveAndFlush(application);
        saveReview(applicationId, previousStatus, application.getApprovalStatus(), null, reason, null, actor.userId(), now);
        recordTransitionAudit(application, previousStatus, reason, null, actor, request);
        saveOutbox(application, "STORE_HELD", actor, now);
        return actionResponse(application);
    }

    @Transactional
    public StoreApprovalDtos.ActionResponse reject(
            Long applicationId,
            StoreApprovalDtos.RejectRequest command,
            AdminActor actor,
            HttpServletRequest request
    ) {
        String reason = validateReason(command.reason());
        String reasonCode = normalizeUpper(command.reasonCode());
        if (!REJECTION_CODES.contains(reasonCode)) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "Unsupported rejection reasonCode.");
        }

        StoreApplication application = findApplication(applicationId);
        assertVersion(application, command.version());
        assertTransitionAllowed(application, Set.of(
                StoreApplication.STATUS_PENDING,
                StoreApplication.STATUS_ON_HOLD,
                StoreApplication.STATUS_APPROVED
        ));

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        String previousStatus = application.getApprovalStatus();
        if (StoreApplication.STATUS_APPROVED.equals(previousStatus)) {
            deactivateRejectedStore(application, reason, actor.userId(), now);
        }
        application.reject(actor.userId(), now);
        application = applicationRepository.saveAndFlush(application);
        saveReview(applicationId, previousStatus, application.getApprovalStatus(), reasonCode, reason, null, actor.userId(), now);
        recordTransitionAudit(application, previousStatus, reason, reasonCode, actor, request);
        saveOutbox(application, "STORE_REJECTED", actor, now);
        return actionResponse(application);
    }

    @Transactional
    public StoreApprovalDtos.DocumentAccessResponse documentAccess(
            Long applicationId,
            Long documentId,
            String rawPurpose,
            AdminActor actor,
            HttpServletRequest request
    ) {
        findApplication(applicationId);
        String purpose = normalizeNullable(rawPurpose);
        if (!"preview".equals(purpose) && !"download".equals(purpose)) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "purpose must be preview or download.");
        }
        StoreApplicationDocument document = applicationDocumentRepository
                .findByIdAndApplicationId(documentId, applicationId)
                .orElseThrow(() -> new AppException(ErrorCode.STORE_DOCUMENT_NOT_FOUND));
        StoreDocumentAccessService.PresignedDocument presigned = documentAccessService.presign(
                document.getObjectKey(),
                document.getOriginalName(),
                purpose
        );
        auditService.record(
                actor,
                "STORE_DOCUMENT_ACCESS_URL_ISSUED",
                "STORE_APPLICATION_DOCUMENT",
                documentId,
                null,
                Map.of("purpose", purpose, "applicationId", applicationId),
                null,
                null,
                request
        );
        return new StoreApprovalDtos.DocumentAccessResponse(presigned.accessUrl(), presigned.expiresAt());
    }

    private Restaurant activateApprovedStore(StoreApplication application, Integer actorUserId) {
        Long storeId = application.getStoreId();
        if (storeId != null) {
            Restaurant restaurant = restaurantRepository.findById(storeId)
                    .orElseThrow(() -> new AppException(ErrorCode.COMMON_NOT_FOUND));
            AdminStoreOperation operation = storeOperationRepository.findById(storeId)
                    .orElseGet(() -> AdminStoreOperation.initial(storeId, defaultVisibility(restaurant)));
            operation.changeOperation("operating", "Store approval restored.", actorUserId);
            storeOperationRepository.save(operation);
            ensureActiveOwner(storeId, application.getApplicantUserId());
            return restaurant;
        }

        Restaurant restaurant = restaurantRepository.save(Restaurant.create(
                application.getStoreName(),
                application.getAddress(),
                application.getPhone(),
                null,
                application.getDescription(),
                "draft"
        ));
        copyApplicationChildren(application.getId(), restaurant.getId());
        ensureActiveOwner(restaurant.getId(), application.getApplicantUserId());
        return restaurant;
    }

    private void deactivateRejectedStore(
            StoreApplication application,
            String reason,
            Integer actorUserId,
            OffsetDateTime now
    ) {
        Long storeId = application.getStoreId();
        if (storeId == null) {
            return;
        }

        restaurantRepository.findById(storeId).ifPresent(restaurant -> {
            restaurant.update(
                    restaurant.getTitle(),
                    restaurant.getAddress(),
                    restaurant.getPhone(),
                    restaurant.getBusinessHours(),
                    restaurant.getIntroduction(),
                    "hidden"
            );
            restaurantRepository.save(restaurant);
        });

        AdminStoreOperation operation = storeOperationRepository.findById(storeId)
                .orElseGet(() -> AdminStoreOperation.initial(storeId, "hidden"));
        operation.changeVisibility("hidden", reason, actorUserId);
        operation.changeOperation("closed", reason, actorUserId);
        storeOperationRepository.save(operation);

        storeOwnerRepository.findByStoreIdAndRevokedAtIsNull(storeId)
                .forEach(owner -> owner.revoke(now));
    }

    private void ensureActiveOwner(Long storeId, Integer userId) {
        storeOwnerRepository.findFirstByStoreIdAndUserIdOrderByCreatedAtDescIdDesc(storeId, userId)
                .ifPresentOrElse(
                        StoreOwner::restore,
                        () -> storeOwnerRepository.save(StoreOwner.createOwner(storeId, userId))
                );
    }

    private String defaultVisibility(Restaurant store) {
        String status = normalizeNullable(store.getExposureStatus());
        return status != null && Set.of("published", "visible").contains(status.toLowerCase(Locale.ROOT))
                ? "visible"
                : "hidden";
    }

    private void copyApplicationChildren(Long applicationId, Long restaurantId) {
        List<StoreApplicationCategory> categories =
                applicationCategoryRepository.findByApplicationIdOrderByDisplayOrderAscIdAsc(applicationId);
        for (StoreApplicationCategory category : categories) {
            restaurantCategoryRepository.save(RestaurantCategory.create(
                    restaurantId,
                    category.getCategoryCode(),
                    category.getDisplayOrder()
            ));
        }
        List<StoreApplicationMenu> menus =
                applicationMenuRepository.findByApplicationIdOrderByDisplayOrderAscIdAsc(applicationId);
        for (StoreApplicationMenu menu : menus) {
            restaurantMenuRepository.save(RestaurantMenu.create(
                    restaurantId,
                    menu.getName(),
                    menu.getPrice(),
                    menu.getDescription(),
                    menu.getDisplayOrder()
            ));
        }
    }

    private void assertDocumentsComplete(Long applicationId) {
        long total = applicationDocumentRepository.countByApplicationId(applicationId);
        long verified = applicationDocumentRepository.countByApplicationIdAndVerificationStatus(
                applicationId,
                StoreApplicationDocument.STATUS_VERIFIED
        );
        if (total == 0 || total != verified) {
            throw new AppException(ErrorCode.STORE_APPROVAL_DOCUMENT_INCOMPLETE);
        }
    }

    private void assertTransitionAllowed(StoreApplication application, Set<String> allowedCurrentStates) {
        if (!allowedCurrentStates.contains(application.getApprovalStatus())) {
            throw new AppException(ErrorCode.STORE_APPROVAL_INVALID_TRANSITION);
        }
    }

    private void assertVersion(StoreApplication application, Long expectedVersion) {
        if (expectedVersion == null || !expectedVersion.equals(application.getVersion())) {
            throw new AppException(ErrorCode.STORE_APPROVAL_VERSION_CONFLICT);
        }
    }

    private void saveReview(
            Long applicationId,
            String previousStatus,
            String nextStatus,
            String reasonCode,
            String reason,
            String comment,
            Integer actorUserId,
            OffsetDateTime now
    ) {
        reviewRepository.save(StoreApplicationReview.create(
                applicationId,
                previousStatus,
                nextStatus,
                reasonCode,
                reason,
                comment,
                actorUserId,
                now,
                MDC.get(RequestIdFilter.MDC_KEY_REQUEST_ID)
        ));
    }

    private void recordTransitionAudit(
            StoreApplication application,
            String previousStatus,
            String reason,
            String reasonCode,
            AdminActor actor,
            HttpServletRequest request
    ) {
        auditService.record(
                actor,
                switch (application.getApprovalStatus()) {
                    case StoreApplication.STATUS_APPROVED -> "STORE_APPROVED";
                    case StoreApplication.STATUS_ON_HOLD -> "STORE_HELD";
                    case StoreApplication.STATUS_REJECTED -> "STORE_REJECTED";
                    default -> "STORE_APPROVAL_STATUS_CHANGED";
                },
                "STORE_APPROVAL",
                application.getId(),
                Map.of("approvalStatus", previousStatus),
                mapOfNullable(
                        "approvalStatus", application.getApprovalStatus(),
                        "storeId", application.getStoreId()
                ),
                reasonCode,
                reason,
                request
        );
    }

    private void saveOutbox(
            StoreApplication application,
            String eventType,
            AdminActor actor,
            OffsetDateTime now
    ) {
        outboxRepository.save(AdminOutboxEvent.pending(
                eventType,
                "STORE_APPROVAL",
                String.valueOf(application.getId()),
                mapOfNullable(
                        "applicationId", application.getId(),
                        "applicantUserId", application.getApplicantUserId(),
                        "actorUsername", actor.username(),
                        "storeId", application.getStoreId(),
                        "approvalStatus", application.getApprovalStatus()
                ),
                now
        ));
    }

    private StoreApprovalDtos.ActionResponse actionResponse(StoreApplication application) {
        return new StoreApprovalDtos.ActionResponse(
                application.getId(),
                application.getApprovalStatus(),
                application.getStoreId(),
                application.getVersion(),
                application.getReviewedAt()
        );
    }

    private StoreApplication findApplication(Long applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new AppException(ErrorCode.STORE_APPROVAL_NOT_FOUND));
    }

    private List<StoreApprovalDtos.CodeName> categories(Long applicationId) {
        return applicationCategoryRepository.findByApplicationIdOrderByDisplayOrderAscIdAsc(applicationId).stream()
                .map(category -> codeName(category.getCategoryCode(), CATEGORY_NAMES))
                .toList();
    }

    private StoreApprovalDtos.CodeName codeName(String code, Map<String, String> labels) {
        String normalized = normalizeUpper(code);
        return new StoreApprovalDtos.CodeName(normalized, labels.getOrDefault(normalized, normalized));
    }

    private Sort parseSort(String rawSort) {
        String value = normalizeNullable(rawSort);
        if (value == null) {
            return Sort.by(Sort.Order.desc("appliedAt"), Sort.Order.desc("id"));
        }
        String[] parts = value.split(",");
        if (parts.length != 2) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "sort must use field,direction format.");
        }
        String property = switch (parts[0].trim()) {
            case "appliedAt" -> "appliedAt";
            case "updatedAt" -> "updatedAt";
            case "id" -> "id";
            default -> throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "Unsupported sort field.");
        };
        Sort.Direction direction;
        try {
            direction = Sort.Direction.fromString(parts[1].trim());
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "sort direction must be asc or desc.");
        }
        return Sort.by(new Sort.Order(direction, property), Sort.Order.desc("id"));
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "page must be >= 0 and size must be between 1 and 100.");
        }
    }

    private OffsetDateTime startOfDay(LocalDate date) {
        return date == null ? null : date.atStartOfDay(SEOUL).toOffsetDateTime();
    }

    private OffsetDateTime startOfNextDay(LocalDate date) {
        return date == null ? null : date.plusDays(1).atStartOfDay(SEOUL).toOffsetDateTime();
    }

    private String validateReason(String value) {
        String normalized = normalizeNullable(value);
        if (normalized == null || normalized.replaceAll("\\s", "").length() < 10 || normalized.length() > 1000) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "reason must contain 10 to 1000 non-blank characters.");
        }
        return normalized;
    }

    private String normalizeEnum(String value, Set<String> allowed, String field) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            return null;
        }
        normalized = normalized.toLowerCase(Locale.ROOT);
        if (!allowed.contains(normalized)) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "Unsupported " + field + ".");
        }
        return normalized;
    }

    private String normalizeUpper(String value) {
        String normalized = normalizeNullable(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private Map<String, Object> mapOfNullable(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            result.put(String.valueOf(values[i]), values[i + 1]);
        }
        return result;
    }
}
