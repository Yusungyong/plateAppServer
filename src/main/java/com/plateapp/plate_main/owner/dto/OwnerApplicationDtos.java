package com.plateapp.plate_main.owner.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public final class OwnerApplicationDtos {

    private OwnerApplicationDtos() {
    }

    public record AccountRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 64) String password,
            @NotBlank @Size(max = 100) String nickname
    ) {
    }

    public record OwnerProfileRequest(
            @NotBlank @Size(max = 100) String ownerName,
            @Size(max = 40) String ownerPhone,
            @Email @Size(max = 320) String ownerEmail
    ) {
    }

    public record BusinessRequest(
            @NotBlank String businessNumber,
            @Size(max = 150) String businessName
    ) {
    }

    public record StoreRequest(
            @NotBlank @Size(max = 150) String storeName,
            @NotBlank @Size(max = 50) String regionCode,
            @NotBlank @Size(max = 300) String address,
            @Size(max = 40) String phone,
            @Email @Size(max = 320) String email,
            String description
    ) {
    }

    public record CategoryRequest(
            @NotBlank @Size(max = 50) String categoryCode,
            Integer displayOrder
    ) {
    }

    public record MenuRequest(
            @NotBlank @Size(max = 150) String name,
            BigDecimal price,
            String description,
            Integer displayOrder
    ) {
    }

    public record SignupApplicationRequest(
            @Valid @NotNull AccountRequest account,
            @Valid @NotNull OwnerProfileRequest ownerProfile,
            @Valid @NotNull BusinessRequest business,
            @Valid @NotNull StoreRequest store,
            @Valid List<CategoryRequest> categories,
            @Valid List<MenuRequest> menus
    ) {
        public StoreApplicationUpsertRequest applicationRequest() {
            return new StoreApplicationUpsertRequest(ownerProfile, business, store, categories, menus);
        }
    }

    public record StoreApplicationUpsertRequest(
            @Valid @NotNull OwnerProfileRequest ownerProfile,
            @Valid @NotNull BusinessRequest business,
            @Valid @NotNull StoreRequest store,
            @Valid List<CategoryRequest> categories,
            @Valid List<MenuRequest> menus
    ) {
    }

    public record SubmitRequest(@NotNull Long version) {
    }

    public record ApplicationCreatedResponse(Long applicationId, String approvalStatus) {
    }

    public record DocumentUploadResponse(
            Long documentId,
            String documentType,
            String originalName,
            String verificationStatus
    ) {
    }

    public record SubmitResponse(
            Long applicationId,
            String approvalStatus,
            String verificationStatus,
            Long version
    ) {
    }

    public record ApplicationListItem(
            Long applicationId,
            String storeName,
            String approvalStatus,
            String verificationStatus,
            OffsetDateTime appliedAt,
            OffsetDateTime updatedAt,
            Long version
    ) {
    }

    public record ApplicationListResponse(
            List<ApplicationListItem> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean hasNext
    ) {
    }

    public record CategoryResponse(String categoryCode, Integer displayOrder) {
    }

    public record MenuResponse(Long id, String name, BigDecimal price, String description, Integer displayOrder) {
    }

    public record DocumentResponse(Long id, String documentType, String originalName, String verificationStatus) {
    }

    public record ApplicationDetailResponse(
            Long applicationId,
            Long parentApplicationId,
            Long storeId,
            OwnerProfileResponse ownerProfile,
            BusinessResponse business,
            StoreResponse store,
            List<CategoryResponse> categories,
            List<MenuResponse> menus,
            List<DocumentResponse> documents,
            String approvalStatus,
            String verificationStatus,
            OffsetDateTime appliedAt,
            OffsetDateTime updatedAt,
            Long version
    ) {
    }

    public record OwnerProfileResponse(String ownerName, String ownerPhone, String ownerEmail) {
    }

    public record BusinessResponse(String businessName, String businessNumber) {
    }

    public record StoreResponse(
            String storeName,
            String regionCode,
            String address,
            String phone,
            String email,
            String description
    ) {
    }
}
