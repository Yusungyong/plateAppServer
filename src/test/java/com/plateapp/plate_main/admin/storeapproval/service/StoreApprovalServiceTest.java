package com.plateapp.plate_main.admin.storeapproval.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.plateapp.plate_main.admin.audit.service.AdminAuditService;
import com.plateapp.plate_main.admin.outbox.entity.AdminOutboxEvent;
import com.plateapp.plate_main.admin.outbox.repository.AdminOutboxEventRepository;
import com.plateapp.plate_main.admin.security.AdminActor;
import com.plateapp.plate_main.admin.storeapproval.dto.StoreApprovalDtos;
import com.plateapp.plate_main.admin.storeapproval.entity.StoreApplication;
import com.plateapp.plate_main.admin.storeapproval.entity.StoreApplicationReview;
import com.plateapp.plate_main.admin.storeapproval.repository.StoreApplicationCategoryRepository;
import com.plateapp.plate_main.admin.storeapproval.repository.StoreApplicationDocumentRepository;
import com.plateapp.plate_main.admin.storeapproval.repository.StoreApplicationMenuRepository;
import com.plateapp.plate_main.admin.storeapproval.repository.StoreApplicationRepository;
import com.plateapp.plate_main.admin.storeapproval.repository.StoreApplicationReviewRepository;
import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import com.plateapp.plate_main.common.s3.S3UploadService;
import com.plateapp.plate_main.owner.entity.StoreOwner;
import com.plateapp.plate_main.owner.repository.StoreOwnerRepository;
import com.plateapp.plate_main.restaurant.entity.Restaurant;
import com.plateapp.plate_main.restaurant.repository.RestaurantCategoryRepository;
import com.plateapp.plate_main.restaurant.repository.RestaurantMenuRepository;
import com.plateapp.plate_main.restaurant.repository.RestaurantRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class StoreApprovalServiceTest {

    @Mock
    private StoreApplicationRepository applicationRepository;
    @Mock
    private StoreApplicationCategoryRepository applicationCategoryRepository;
    @Mock
    private StoreApplicationMenuRepository applicationMenuRepository;
    @Mock
    private StoreApplicationDocumentRepository applicationDocumentRepository;
    @Mock
    private StoreApplicationReviewRepository reviewRepository;
    @Mock
    private RestaurantRepository restaurantRepository;
    @Mock
    private RestaurantCategoryRepository restaurantCategoryRepository;
    @Mock
    private RestaurantMenuRepository restaurantMenuRepository;
    @Mock
    private AdminOutboxEventRepository outboxRepository;
    @Mock
    private AdminAuditService auditService;
    @Mock
    private BusinessNumberCrypto businessNumberCrypto;
    @Mock
    private S3UploadService s3UploadService;
    @Mock
    private StoreDocumentAccessService documentAccessService;
    @Mock
    private StoreOwnerRepository storeOwnerRepository;
    @Mock
    private HttpServletRequest request;

    private StoreApprovalService service;

    @BeforeEach
    void setUp() {
        service = new StoreApprovalService(
                applicationRepository,
                applicationCategoryRepository,
                applicationMenuRepository,
                applicationDocumentRepository,
                reviewRepository,
                restaurantRepository,
                restaurantCategoryRepository,
                restaurantMenuRepository,
                outboxRepository,
                auditService,
                businessNumberCrypto,
                s3UploadService,
                documentAccessService,
                storeOwnerRepository
        );
    }

    @Test
    void listUsesNonNullSearchParametersWhenKeywordIsMissing() {
        when(applicationRepository.search(
                eq(false), eq(""), eq(""), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                any(Pageable.class)
        )).thenReturn(Page.empty());

        StoreApprovalDtos.ListResponse response = service.list(
                0, 20, null, null, null, null, null, null, null, "appliedAt,desc"
        );

        assertEquals(0, response.totalElements());
        verify(applicationRepository).search(
                eq(false), eq(""), eq(""), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                any(Pageable.class)
        );
        verify(businessNumberCrypto, never()).hash(anyString());
    }

    @Test
    void listBuildsLowercasePatternAndBusinessNumberHashWhenKeywordExists() {
        when(businessNumberCrypto.hash("Plate 123")).thenReturn("business-hash");
        when(applicationRepository.search(
                eq(true), eq("%plate 123%"), eq("business-hash"), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), any(Pageable.class)
        )).thenReturn(Page.empty());

        service.list(0, 20, "  Plate 123  ", null, null, null, null, null, null, "appliedAt,desc");

        verify(applicationRepository).search(
                eq(true), eq("%plate 123%"), eq("business-hash"), isNull(), isNull(), isNull(), isNull(),
                isNull(), isNull(), any(Pageable.class)
        );
    }

    @Test
    void approveStopsBeforeCreatingStoreWhenVersionIsStale() {
        StoreApplication application = application(10L, 3L, StoreApplication.STATUS_PENDING,
                StoreApplication.VERIFICATION_VERIFIED);
        when(applicationRepository.findById(10L)).thenReturn(Optional.of(application));

        AppException exception = assertThrows(
                AppException.class,
                () -> service.approve(
                        10L,
                        new StoreApprovalDtos.ApproveRequest(2L, null),
                        actor(),
                        request
                )
        );

        assertEquals(ErrorCode.STORE_APPROVAL_VERSION_CONFLICT, exception.getErrorCode());
        assertEquals(HttpStatus.CONFLICT, exception.getErrorCode().getStatus());
        verify(restaurantRepository, never()).save(any(Restaurant.class));
        verify(reviewRepository, never()).save(any(StoreApplicationReview.class));
        verify(outboxRepository, never()).save(any(AdminOutboxEvent.class));
    }

    @Test
    void approveCreatesDraftStoreAndRecordsDecision() {
        StoreApplication application = application(10L, 3L, StoreApplication.STATUS_PENDING,
                StoreApplication.VERIFICATION_VERIFIED);
        Restaurant savedRestaurant = Restaurant.create(
                "Plate Kitchen",
                "Seoul",
                "02-1234-5678",
                null,
                "Introduction",
                "draft"
        );
        ReflectionTestUtils.setField(savedRestaurant, "id", 55L);

        when(applicationRepository.findById(10L)).thenReturn(Optional.of(application));
        when(applicationDocumentRepository.countByApplicationId(10L)).thenReturn(2L);
        when(applicationDocumentRepository.countByApplicationIdAndVerificationStatus(10L, "verified")).thenReturn(2L);
        when(applicationRepository.existsByBusinessNumberHashAndApprovalStatusAndIdNot(
                "business-hash",
                StoreApplication.STATUS_APPROVED,
                10L
        )).thenReturn(false);
        when(restaurantRepository.save(any(Restaurant.class))).thenReturn(savedRestaurant);
        when(applicationCategoryRepository.findByApplicationIdOrderByDisplayOrderAscIdAsc(10L))
                .thenReturn(List.of());
        when(applicationMenuRepository.findByApplicationIdOrderByDisplayOrderAscIdAsc(10L))
                .thenReturn(List.of());
        when(applicationRepository.saveAndFlush(application)).thenReturn(application);

        StoreApprovalDtos.ActionResponse response = service.approve(
                10L,
                new StoreApprovalDtos.ApproveRequest(3L, "verified"),
                actor(),
                request
        );

        ArgumentCaptor<Restaurant> restaurantCaptor = ArgumentCaptor.forClass(Restaurant.class);
        verify(restaurantRepository).save(restaurantCaptor.capture());
        assertEquals("draft", restaurantCaptor.getValue().getExposureStatus());
        assertEquals(StoreApplication.STATUS_APPROVED, response.approvalStatus());
        assertEquals(55L, response.storeId());
        verify(reviewRepository).save(any(StoreApplicationReview.class));
        verify(storeOwnerRepository).save(any(StoreOwner.class));
        verify(auditService).record(
                any(), any(), any(), any(), any(), any(), any(), any(), any()
        );
        verify(outboxRepository).save(any(AdminOutboxEvent.class));
    }

    @Test
    void approveRequiresCompletedBusinessVerification() {
        StoreApplication application = application(10L, 3L, StoreApplication.STATUS_PENDING, "reviewing");
        when(applicationRepository.findById(10L)).thenReturn(Optional.of(application));

        AppException exception = assertThrows(
                AppException.class,
                () -> service.approve(
                        10L,
                        new StoreApprovalDtos.ApproveRequest(3L, null),
                        actor(),
                        request
                )
        );

        assertEquals(ErrorCode.STORE_APPROVAL_VERIFICATION_INCOMPLETE, exception.getErrorCode());
        verify(restaurantRepository, never()).save(any(Restaurant.class));
    }

    private StoreApplication application(Long id, Long version, String status, String verificationStatus) {
        StoreApplication application = BeanUtils.instantiateClass(StoreApplication.class);
        ReflectionTestUtils.setField(application, "id", id);
        ReflectionTestUtils.setField(application, "applicantUserId", 100);
        ReflectionTestUtils.setField(application, "storeName", "Plate Kitchen");
        ReflectionTestUtils.setField(application, "regionCode", "SEOUL");
        ReflectionTestUtils.setField(application, "address", "Seoul");
        ReflectionTestUtils.setField(application, "phone", "02-1234-5678");
        ReflectionTestUtils.setField(application, "ownerName", "Owner");
        ReflectionTestUtils.setField(application, "businessNumberHash", "business-hash");
        ReflectionTestUtils.setField(application, "approvalStatus", status);
        ReflectionTestUtils.setField(application, "verificationStatus", verificationStatus);
        ReflectionTestUtils.setField(application, "description", "Introduction");
        ReflectionTestUtils.setField(application, "appliedAt", OffsetDateTime.now(ZoneOffset.UTC));
        ReflectionTestUtils.setField(application, "updatedAt", OffsetDateTime.now(ZoneOffset.UTC));
        ReflectionTestUtils.setField(application, "version", version);
        return application;
    }

    private AdminActor actor() {
        return new AdminActor(1, "admin@example.com", "ADMIN");
    }
}
