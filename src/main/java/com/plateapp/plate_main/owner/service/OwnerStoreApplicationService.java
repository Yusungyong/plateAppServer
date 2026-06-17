package com.plateapp.plate_main.owner.service;

import com.plateapp.plate_main.admin.storeapproval.entity.StoreApplication;
import com.plateapp.plate_main.admin.storeapproval.entity.StoreApplicationCategory;
import com.plateapp.plate_main.admin.storeapproval.entity.StoreApplicationDocument;
import com.plateapp.plate_main.admin.storeapproval.entity.StoreApplicationMenu;
import com.plateapp.plate_main.admin.storeapproval.repository.StoreApplicationCategoryRepository;
import com.plateapp.plate_main.admin.storeapproval.repository.StoreApplicationDocumentRepository;
import com.plateapp.plate_main.admin.storeapproval.repository.StoreApplicationMenuRepository;
import com.plateapp.plate_main.admin.storeapproval.repository.StoreApplicationRepository;
import com.plateapp.plate_main.admin.storeapproval.service.BusinessNumberCrypto;
import com.plateapp.plate_main.auth.domain.User;
import com.plateapp.plate_main.auth.dto.SignupRequest;
import com.plateapp.plate_main.auth.repository.UserRepository;
import com.plateapp.plate_main.auth.service.AuthService;
import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import com.plateapp.plate_main.common.s3.S3UploadService;
import com.plateapp.plate_main.owner.dto.OwnerApplicationDtos;
import com.plateapp.plate_main.owner.entity.BusinessProfile;
import com.plateapp.plate_main.owner.repository.BusinessProfileRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class OwnerStoreApplicationService {

    private static final Set<String> EDITABLE_STATUSES = Set.of(
            StoreApplication.STATUS_DRAFT,
            StoreApplication.STATUS_ON_HOLD
    );
    private static final Set<String> ACTIVE_DUPLICATE_STATUSES = Set.of(
            StoreApplication.STATUS_PENDING,
            StoreApplication.STATUS_ON_HOLD,
            StoreApplication.STATUS_APPROVED
    );
    private static final Set<String> DOCUMENT_TYPES = Set.of(
            "business_registration",
            "sales_permit",
            "identity_verification",
            "other"
    );
    private static final Set<String> DOCUMENT_MIME_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png"
    );

    private final AuthService authService;
    private final UserRepository userRepository;
    private final BusinessProfileRepository businessProfileRepository;
    private final StoreApplicationRepository applicationRepository;
    private final StoreApplicationCategoryRepository categoryRepository;
    private final StoreApplicationMenuRepository menuRepository;
    private final StoreApplicationDocumentRepository documentRepository;
    private final BusinessNumberCrypto businessNumberCrypto;
    private final S3UploadService s3UploadService;

    @Value("${aws.s3.restaurantFilePath:restaurants/}")
    private String restaurantFilePrefix;

    @Transactional
    public OwnerApplicationDtos.ApplicationCreatedResponse signupAndCreate(
            OwnerApplicationDtos.SignupApplicationRequest request
    ) {
        String username = normalizeRequired(request.account().username(), "account.username is required.");
        String email = normalizeRequired(request.account().email(), "account.email is required.");
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setUsername(username);
        signupRequest.setEmail(email);
        signupRequest.setPassword(request.account().password());
        signupRequest.setNickname(request.account().nickname());
        authService.signup(signupRequest);

        Integer userId = findUserIdByUsername(username);
        StoreApplication application = createApplication(userId, request.applicationRequest());
        return new OwnerApplicationDtos.ApplicationCreatedResponse(
                application.getId(),
                application.getApprovalStatus()
        );
    }

    @Transactional
    public OwnerApplicationDtos.ApplicationCreatedResponse create(String username, OwnerApplicationDtos.StoreApplicationUpsertRequest request) {
        Integer userId = currentUserId(username);
        StoreApplication application = createApplication(userId, request);
        return new OwnerApplicationDtos.ApplicationCreatedResponse(
                application.getId(),
                application.getApprovalStatus()
        );
    }

    @Transactional(readOnly = true)
    public OwnerApplicationDtos.ApplicationListResponse list(String username, int page, int size) {
        Integer userId = currentUserId(username);
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));
        Page<StoreApplication> result = applicationRepository.findByApplicantUserId(
                userId,
                PageRequest.of(
                        safePage,
                        safeSize,
                        Sort.by(Sort.Order.desc("appliedAt"), Sort.Order.desc("id"))
                )
        );

        return new OwnerApplicationDtos.ApplicationListResponse(
                result.getContent().stream().map(application -> new OwnerApplicationDtos.ApplicationListItem(
                        application.getId(),
                        application.getStoreName(),
                        application.getApprovalStatus(),
                        application.getVerificationStatus(),
                        application.getAppliedAt(),
                        application.getUpdatedAt(),
                        application.getVersion()
                )).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.hasNext()
        );
    }

    @Transactional(readOnly = true)
    public OwnerApplicationDtos.ApplicationDetailResponse detail(String username, Long applicationId) {
        Integer userId = currentUserId(username);
        StoreApplication application = findOwnApplication(applicationId, userId);
        return toDetail(application);
    }

    @Transactional
    public OwnerApplicationDtos.ApplicationDetailResponse update(
            String username,
            Long applicationId,
            OwnerApplicationDtos.StoreApplicationUpsertRequest request
    ) {
        Integer userId = currentUserId(username);
        StoreApplication application = findOwnApplication(applicationId, userId);
        assertEditable(application);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        UpsertData data = validateUpsert(request);
        assertNoActiveDuplicate(data.businessNumberHash(), application.getId());

        upsertBusinessProfile(userId, data.ownerName(), data.ownerPhone(), data.ownerEmail());
        application.updateDraft(
                data.storeName(),
                data.regionCode(),
                data.address(),
                data.phone(),
                data.email(),
                data.ownerName(),
                data.description(),
                now
        );
        application.replaceBusinessNumber(data.businessNumberEncrypted(), data.businessNumberHash(), now);
        application.updateBusinessName(data.businessName(), now);
        application.updateBusinessVerification(
                data.businessRepresentativeName(),
                data.businessOpeningDate(),
                data.businessVerificationProvider(),
                data.businessVerificationStatus(),
                data.businessVerifiedAt(),
                data.businessVerificationMessage(),
                now
        );
        replaceChildren(application.getId(), data.categories(), data.menus());
        return toDetail(applicationRepository.saveAndFlush(application));
    }

    @Transactional
    public OwnerApplicationDtos.DocumentUploadResponse uploadDocument(
            String username,
            Long applicationId,
            String rawDocumentType,
            MultipartFile file
    ) {
        Integer userId = currentUserId(username);
        StoreApplication application = findOwnApplication(applicationId, userId);
        assertEditable(application);

        String documentType = normalizeDocumentType(rawDocumentType);
        validateDocumentFile(file);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        String objectKey = uploadPrivateDocument(applicationId, documentType, file);
        documentRepository.findByApplicationIdAndDocumentType(applicationId, documentType)
                .ifPresent(existing -> {
                    s3UploadService.deleteObjectByKey(existing.getObjectKey());
                    documentRepository.delete(existing);
                    documentRepository.flush();
                });

        StoreApplicationDocument document = documentRepository.save(StoreApplicationDocument.submitted(
                applicationId,
                documentType,
                objectKey,
                normalizeFilename(file.getOriginalFilename()),
                normalizeNullable(file.getContentType()),
                file.getSize(),
                now
        ));

        return new OwnerApplicationDtos.DocumentUploadResponse(
                document.getId(),
                document.getDocumentType(),
                document.getOriginalName(),
                document.getVerificationStatus()
        );
    }

    @Transactional
    public OwnerApplicationDtos.SubmitResponse submit(
            String username,
            Long applicationId,
            OwnerApplicationDtos.SubmitRequest request
    ) {
        Integer userId = currentUserId(username);
        StoreApplication application = findOwnApplication(applicationId, userId);
        assertEditable(application);
        assertVersion(application, request.version());
        assertNoActiveDuplicate(application.getBusinessNumberHash(), application.getId());
        assertSubmittable(application);

        application.submit(OffsetDateTime.now(ZoneOffset.UTC));
        application = applicationRepository.saveAndFlush(application);
        return new OwnerApplicationDtos.SubmitResponse(
                application.getId(),
                application.getApprovalStatus(),
                application.getVerificationStatus(),
                application.getVersion()
        );
    }

    private StoreApplication createApplication(Integer userId, OwnerApplicationDtos.StoreApplicationUpsertRequest request) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        UpsertData data = validateUpsert(request);
        assertNoActiveDuplicate(data.businessNumberHash(), null);
        upsertBusinessProfile(userId, data.ownerName(), data.ownerPhone(), data.ownerEmail());

        Long parentApplicationId = applicationRepository
                .findFirstByApplicantUserIdAndBusinessNumberHashAndApprovalStatusOrderByAppliedAtDescIdDesc(
                        userId,
                        data.businessNumberHash(),
                        StoreApplication.STATUS_REJECTED
                )
                .map(StoreApplication::getId)
                .orElse(null);

        StoreApplication application = applicationRepository.saveAndFlush(StoreApplication.createDraft(
                parentApplicationId,
                userId,
                data.storeName(),
                data.regionCode(),
                data.address(),
                data.phone(),
                data.email(),
                data.ownerName(),
                data.businessNumberEncrypted(),
                data.businessNumberHash(),
                data.businessName(),
                data.description(),
                now
        ));
        application.updateBusinessVerification(
                data.businessRepresentativeName(),
                data.businessOpeningDate(),
                data.businessVerificationProvider(),
                data.businessVerificationStatus(),
                data.businessVerifiedAt(),
                data.businessVerificationMessage(),
                now
        );
        replaceChildren(application.getId(), data.categories(), data.menus());
        return application;
    }

    private void upsertBusinessProfile(Integer userId, String ownerName, String ownerPhone, String ownerEmail) {
        BusinessProfile profile = businessProfileRepository.findByUserId(userId)
                .orElseGet(() -> BusinessProfile.create(userId, ownerName, ownerPhone, ownerEmail));
        profile.update(ownerName, ownerPhone, ownerEmail);
        businessProfileRepository.save(profile);
    }

    private void replaceChildren(
            Long applicationId,
            List<OwnerApplicationDtos.CategoryRequest> categories,
            List<OwnerApplicationDtos.MenuRequest> menus
    ) {
        categoryRepository.deleteByApplicationId(applicationId);
        menuRepository.deleteByApplicationId(applicationId);
        categoryRepository.flush();
        menuRepository.flush();

        for (OwnerApplicationDtos.CategoryRequest category : categories) {
            categoryRepository.save(StoreApplicationCategory.create(
                    applicationId,
                    normalizeUpper(category.categoryCode()),
                    defaultDisplayOrder(category.displayOrder())
            ));
        }
        for (OwnerApplicationDtos.MenuRequest menu : menus) {
            menuRepository.save(StoreApplicationMenu.create(
                    applicationId,
                    normalizeRequired(menu.name(), "menus[].name is required."),
                    normalizePrice(menu.price()),
                    normalizeNullable(menu.description()),
                    defaultDisplayOrder(menu.displayOrder())
            ));
        }
    }

    private OwnerApplicationDtos.ApplicationDetailResponse toDetail(StoreApplication application) {
        Long applicationId = application.getId();
        BusinessProfile profile = businessProfileRepository.findByUserId(application.getApplicantUserId()).orElse(null);
        return new OwnerApplicationDtos.ApplicationDetailResponse(
                applicationId,
                application.getParentApplicationId(),
                application.getStoreId(),
                new OwnerApplicationDtos.OwnerProfileResponse(
                        application.getOwnerName(),
                        profile == null ? null : profile.getOwnerPhone(),
                        profile == null ? null : profile.getOwnerEmail()
                ),
                new OwnerApplicationDtos.BusinessResponse(
                        application.getBusinessName(),
                        businessNumberCrypto.maskEncrypted(application.getBusinessNumberEncrypted()),
                        application.getBusinessRepresentativeName(),
                        application.getBusinessOpeningDate(),
                        application.getBusinessVerificationProvider(),
                        application.getBusinessVerificationStatus(),
                        application.getBusinessVerifiedAt(),
                        application.getBusinessVerificationMessage()
                ),
                new OwnerApplicationDtos.StoreResponse(
                        application.getStoreName(),
                        application.getRegionCode(),
                        application.getAddress(),
                        application.getPhone(),
                        application.getEmail(),
                        application.getDescription()
                ),
                categoryRepository.findByApplicationIdOrderByDisplayOrderAscIdAsc(applicationId).stream()
                        .map(category -> new OwnerApplicationDtos.CategoryResponse(
                                category.getCategoryCode(),
                                category.getDisplayOrder()
                        ))
                        .toList(),
                menuRepository.findByApplicationIdOrderByDisplayOrderAscIdAsc(applicationId).stream()
                        .map(menu -> new OwnerApplicationDtos.MenuResponse(
                                menu.getId(),
                                menu.getName(),
                                menu.getPrice(),
                                menu.getDescription(),
                                menu.getDisplayOrder()
                        ))
                        .toList(),
                documentRepository.findByApplicationIdOrderByCreatedAtAscIdAsc(applicationId).stream()
                        .map(document -> new OwnerApplicationDtos.DocumentResponse(
                                document.getId(),
                                document.getDocumentType(),
                                document.getOriginalName(),
                                document.getVerificationStatus()
                        ))
                        .toList(),
                application.getApprovalStatus(),
                application.getVerificationStatus(),
                application.getAppliedAt(),
                application.getUpdatedAt(),
                application.getVersion()
        );
    }

    private UpsertData validateUpsert(OwnerApplicationDtos.StoreApplicationUpsertRequest request) {
        String businessNumber = normalizeBusinessNumber(request.business().businessNumber());
        String businessNumberHash = businessNumberCrypto.hash(businessNumber);
        return new UpsertData(
                normalizeRequired(request.ownerProfile().ownerName(), "ownerProfile.ownerName is required."),
                normalizeNullable(request.ownerProfile().ownerPhone()),
                normalizeNullable(request.ownerProfile().ownerEmail()),
                businessNumberCrypto.encrypt(businessNumber),
                businessNumberHash,
                normalizeNullable(request.business().businessName()),
                normalizeNullable(request.business().representativeName()),
                request.business().openingDate(),
                normalizeUpperNullable(request.business().verificationProvider()),
                normalizeLowerNullable(request.business().verificationStatus()),
                request.business().verificationVerifiedAt(),
                normalizeNullable(request.business().verificationMessage()),
                normalizeRequired(request.store().storeName(), "store.storeName is required."),
                normalizeUpperRequired(request.store().regionCode(), "store.regionCode is required."),
                normalizeRequired(request.store().address(), "store.address is required."),
                normalizeNullable(request.store().phone()),
                normalizeNullable(request.store().email()),
                normalizeNullable(request.store().description()),
                nullToEmpty(request.categories()),
                nullToEmpty(request.menus())
        );
    }

    private void assertSubmittable(StoreApplication application) {
        boolean hasVerifiedBusiness = "verified".equals(application.getBusinessVerificationStatus());
        boolean hasBusinessRegistrationDocument = documentRepository.existsByApplicationIdAndDocumentType(
                application.getId(),
                "business_registration"
        );
        if (!hasVerifiedBusiness && !hasBusinessRegistrationDocument) {
            throw new AppException(ErrorCode.STORE_APPROVAL_DOCUMENT_INCOMPLETE);
        }
        if (isBlank(application.getStoreName())
                || isBlank(application.getRegionCode())
                || isBlank(application.getAddress())
                || isBlank(application.getOwnerName())
                || application.getBusinessNumberEncrypted() == null
                || isBlank(application.getBusinessNumberHash())) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "필수 신청 정보가 누락되었습니다.");
        }
    }

    private void assertEditable(StoreApplication application) {
        if (!EDITABLE_STATUSES.contains(application.getApprovalStatus())) {
            throw new AppException(ErrorCode.STORE_APPROVAL_INVALID_TRANSITION);
        }
    }

    private void assertVersion(StoreApplication application, Long expectedVersion) {
        if (expectedVersion == null || !expectedVersion.equals(application.getVersion())) {
            throw new AppException(ErrorCode.STORE_APPROVAL_VERSION_CONFLICT);
        }
    }

    private void assertNoActiveDuplicate(String businessNumberHash, Long excludingApplicationId) {
        boolean exists = excludingApplicationId == null
                ? applicationRepository.existsByBusinessNumberHashAndApprovalStatusIn(
                        businessNumberHash,
                        ACTIVE_DUPLICATE_STATUSES
                )
                : applicationRepository.existsByBusinessNumberHashAndApprovalStatusInAndIdNot(
                        businessNumberHash,
                        ACTIVE_DUPLICATE_STATUSES,
                        excludingApplicationId
                );
        if (exists) {
            throw new AppException(ErrorCode.STORE_APPLICATION_DUPLICATE_BUSINESS);
        }
    }

    private StoreApplication findOwnApplication(Long applicationId, Integer userId) {
        return applicationRepository.findByIdAndApplicantUserId(applicationId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.STORE_APPROVAL_NOT_FOUND));
    }

    private Integer currentUserId(String username) {
        userRepository.findById(username)
                .orElseThrow(() -> new AppException(ErrorCode.AUTH_UNAUTHORIZED));
        return findUserIdByUsername(username);
    }

    private Integer findUserIdByUsername(String username) {
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

    private String uploadPrivateDocument(Long applicationId, String documentType, MultipartFile file) {
        try {
            return s3UploadService.uploadStreamKeyWithPrefix(
                    normalizePrefix(restaurantFilePrefix) + "applications/" + applicationId + "/" + documentType + "/",
                    normalizeFilename(file.getOriginalFilename()),
                    file.getInputStream(),
                    file.getSize(),
                    file.getContentType()
            );
        } catch (IOException e) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "파일을 읽을 수 없습니다.");
        }
    }

    private void validateDocumentFile(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() < 1) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "file is required.");
        }
        String contentType = normalizeNullable(file.getContentType());
        if (contentType != null && !DOCUMENT_MIME_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "지원하지 않는 파일 타입입니다.");
        }
    }

    private String normalizeDocumentType(String value) {
        String normalized = normalizeRequired(value, "documentType is required.").toLowerCase(Locale.ROOT);
        if (!DOCUMENT_TYPES.contains(normalized)) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "Unsupported documentType.");
        }
        return normalized;
    }

    private String normalizeBusinessNumber(String value) {
        String normalized = value == null ? "" : value.replaceAll("[^0-9]", "");
        if (normalized.length() != 10) {
            throw new AppException(ErrorCode.BUSINESS_NUMBER_INVALID);
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

    private String normalizeUpperRequired(String value, String message) {
        return normalizeRequired(value, message).toUpperCase(Locale.ROOT);
    }

    private String normalizeUpperNullable(String value) {
        String normalized = normalizeNullable(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private String normalizeLowerNullable(String value) {
        String normalized = normalizeNullable(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private String normalizeUpper(String value) {
        return normalizeRequired(value, "code is required.").toUpperCase(Locale.ROOT);
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
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private String normalizeFilename(String value) {
        String normalized = normalizeNullable(value);
        return normalized == null ? "upload.bin" : normalized;
    }

    private String normalizePrefix(String prefix) {
        String normalized = normalizeNullable(prefix);
        if (normalized == null) {
            return "";
        }
        normalized = normalized.startsWith("/") ? normalized.substring(1) : normalized;
        return normalized.endsWith("/") ? normalized : normalized + "/";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private <T> List<T> nullToEmpty(List<T> values) {
        return values == null ? List.of() : values;
    }

    private record UpsertData(
            String ownerName,
            String ownerPhone,
            String ownerEmail,
            byte[] businessNumberEncrypted,
            String businessNumberHash,
            String businessName,
            String businessRepresentativeName,
            LocalDate businessOpeningDate,
            String businessVerificationProvider,
            String businessVerificationStatus,
            OffsetDateTime businessVerifiedAt,
            String businessVerificationMessage,
            String storeName,
            String regionCode,
            String address,
            String phone,
            String email,
            String description,
            List<OwnerApplicationDtos.CategoryRequest> categories,
            List<OwnerApplicationDtos.MenuRequest> menus
    ) {
    }
}
