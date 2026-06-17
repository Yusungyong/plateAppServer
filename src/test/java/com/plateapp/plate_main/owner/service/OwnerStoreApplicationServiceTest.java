package com.plateapp.plate_main.owner.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.plateapp.plate_main.admin.storeapproval.entity.StoreApplication;
import com.plateapp.plate_main.admin.storeapproval.repository.StoreApplicationCategoryRepository;
import com.plateapp.plate_main.admin.storeapproval.repository.StoreApplicationDocumentRepository;
import com.plateapp.plate_main.admin.storeapproval.repository.StoreApplicationMenuRepository;
import com.plateapp.plate_main.admin.storeapproval.repository.StoreApplicationRepository;
import com.plateapp.plate_main.admin.storeapproval.service.BusinessNumberCrypto;
import com.plateapp.plate_main.auth.domain.User;
import com.plateapp.plate_main.auth.repository.UserRepository;
import com.plateapp.plate_main.auth.service.AuthService;
import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import com.plateapp.plate_main.common.s3.S3UploadService;
import com.plateapp.plate_main.owner.dto.OwnerApplicationDtos;
import com.plateapp.plate_main.owner.repository.BusinessProfileRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OwnerStoreApplicationServiceTest {

    @Mock
    private AuthService authService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private BusinessProfileRepository businessProfileRepository;
    @Mock
    private StoreApplicationRepository applicationRepository;
    @Mock
    private StoreApplicationCategoryRepository categoryRepository;
    @Mock
    private StoreApplicationMenuRepository menuRepository;
    @Mock
    private StoreApplicationDocumentRepository documentRepository;
    @Mock
    private BusinessNumberCrypto businessNumberCrypto;
    @Mock
    private S3UploadService s3UploadService;

    private OwnerStoreApplicationService service;

    @BeforeEach
    void setUp() {
        service = new OwnerStoreApplicationService(
                authService,
                userRepository,
                businessProfileRepository,
                applicationRepository,
                categoryRepository,
                menuRepository,
                documentRepository,
                businessNumberCrypto,
                s3UploadService
        );
    }

    @Test
    void createRejectsInvalidBusinessNumber() {
        stubCurrentUser();

        AppException exception = assertThrows(
                AppException.class,
                () -> service.create("owner@example.com", requestWithBusinessNumber("123-45"))
        );

        assertEquals(ErrorCode.BUSINESS_NUMBER_INVALID, exception.getErrorCode());
    }

    @Test
    void submitRequiresBusinessRegistrationDocument() {
        stubCurrentUser();
        StoreApplication application = draftApplication();
        when(applicationRepository.findByIdAndApplicantUserId(10L, 100)).thenReturn(Optional.of(application));
        when(applicationRepository.existsByBusinessNumberHashAndApprovalStatusInAndIdNot(
                "business-hash",
                Set.of(
                        StoreApplication.STATUS_PENDING,
                        StoreApplication.STATUS_ON_HOLD,
                        StoreApplication.STATUS_APPROVED
                ),
                10L
        )).thenReturn(false);
        when(documentRepository.existsByApplicationIdAndDocumentType(10L, "business_registration")).thenReturn(false);

        AppException exception = assertThrows(
                AppException.class,
                () -> service.submit("owner@example.com", 10L, new OwnerApplicationDtos.SubmitRequest(3L))
        );

        assertEquals(ErrorCode.STORE_APPROVAL_DOCUMENT_INCOMPLETE, exception.getErrorCode());
    }

    @Test
    void submitAllowsVerifiedBusinessWithoutRegistrationDocument() {
        stubCurrentUser();
        StoreApplication application = draftApplication();
        ReflectionTestUtils.setField(application, "businessVerificationStatus", "verified");
        when(applicationRepository.findByIdAndApplicantUserId(10L, 100)).thenReturn(Optional.of(application));
        when(applicationRepository.existsByBusinessNumberHashAndApprovalStatusInAndIdNot(
                "business-hash",
                Set.of(
                        StoreApplication.STATUS_PENDING,
                        StoreApplication.STATUS_ON_HOLD,
                        StoreApplication.STATUS_APPROVED
                ),
                10L
        )).thenReturn(false);
        when(documentRepository.existsByApplicationIdAndDocumentType(10L, "business_registration")).thenReturn(false);
        when(applicationRepository.saveAndFlush(application)).thenReturn(application);

        OwnerApplicationDtos.SubmitResponse response = service.submit(
                "owner@example.com",
                10L,
                new OwnerApplicationDtos.SubmitRequest(3L)
        );

        assertEquals(StoreApplication.STATUS_PENDING, response.approvalStatus());
    }

    private void stubCurrentUser() {
        User user = User.builder()
                .username("owner@example.com")
                .tokenVersion(0)
                .build();
        when(userRepository.findById("owner@example.com")).thenReturn(Optional.of(user));
        when(userRepository.findUserIdByUsername("owner@example.com")).thenReturn(100);
    }

    private OwnerApplicationDtos.StoreApplicationUpsertRequest requestWithBusinessNumber(String businessNumber) {
        return new OwnerApplicationDtos.StoreApplicationUpsertRequest(
                new OwnerApplicationDtos.OwnerProfileRequest("Owner", "010-1234-5678", "owner@example.com"),
                new OwnerApplicationDtos.BusinessRequest(
                        businessNumber,
                        "Plate Company",
                        "Owner",
                        null,
                        null,
                        null,
                        null,
                        null
                ),
                new OwnerApplicationDtos.StoreRequest(
                        "Plate Kitchen",
                        "SEOUL",
                        "Seoul",
                        "02-1234-5678",
                        "store@example.com",
                        "Introduction"
                ),
                List.of(new OwnerApplicationDtos.CategoryRequest("KOREAN", 0)),
                List.of(new OwnerApplicationDtos.MenuRequest("Signature", null, null, 0))
        );
    }

    private StoreApplication draftApplication() {
        StoreApplication application = BeanUtils.instantiateClass(StoreApplication.class);
        ReflectionTestUtils.setField(application, "id", 10L);
        ReflectionTestUtils.setField(application, "applicantUserId", 100);
        ReflectionTestUtils.setField(application, "storeName", "Plate Kitchen");
        ReflectionTestUtils.setField(application, "regionCode", "SEOUL");
        ReflectionTestUtils.setField(application, "address", "Seoul");
        ReflectionTestUtils.setField(application, "ownerName", "Owner");
        ReflectionTestUtils.setField(application, "businessNumberEncrypted", new byte[] {1, 2, 3});
        ReflectionTestUtils.setField(application, "businessNumberHash", "business-hash");
        ReflectionTestUtils.setField(application, "approvalStatus", StoreApplication.STATUS_DRAFT);
        ReflectionTestUtils.setField(application, "verificationStatus", StoreApplication.VERIFICATION_NOT_REQUESTED);
        ReflectionTestUtils.setField(application, "appliedAt", OffsetDateTime.now(ZoneOffset.UTC));
        ReflectionTestUtils.setField(application, "updatedAt", OffsetDateTime.now(ZoneOffset.UTC));
        ReflectionTestUtils.setField(application, "version", 3L);
        return application;
    }
}
