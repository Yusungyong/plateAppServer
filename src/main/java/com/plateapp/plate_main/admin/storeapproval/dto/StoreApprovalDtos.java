package com.plateapp.plate_main.admin.storeapproval.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public final class StoreApprovalDtos {

    private StoreApprovalDtos() {
    }

    public record CodeName(String code, String name) {
    }

    public record ListItem(
            Long id,
            String name,
            List<CodeName> categories,
            CodeName region,
            String address,
            String ownerName,
            String approvalStatus,
            String verificationStatus,
            OffsetDateTime appliedAt,
            OffsetDateTime updatedAt
    ) {
    }

    public record ListResponse(
            List<ListItem> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean hasNext
    ) {
    }

    public record MenuItem(Long id, String name, BigDecimal price, String description) {
    }

    public record DocumentItem(Long id, String type, String name, String status) {
    }

    public record DetailResponse(
            Long id,
            String name,
            List<CodeName> categories,
            CodeName region,
            String address,
            String phone,
            String email,
            String ownerName,
            String businessNumber,
            String approvalStatus,
            String verificationStatus,
            String mainImageUrl,
            String description,
            List<MenuItem> representativeMenus,
            List<DocumentItem> documents,
            OffsetDateTime appliedAt,
            OffsetDateTime updatedAt,
            String reviewReason,
            Long storeId,
            Long version
    ) {
    }

    public record ApproveRequest(
            @NotNull Long version,
            @Size(max = 1000) String comment
    ) {
    }

    public record HoldRequest(
            @NotNull Long version,
            @NotBlank @Size(min = 10, max = 1000) String reason
    ) {
    }

    public record RejectRequest(
            @NotNull Long version,
            @NotBlank String reasonCode,
            @NotBlank @Size(min = 10, max = 1000) String reason
    ) {
    }

    public record ChangeRequestItemRequest(
            @NotBlank @Size(max = 120) String field,
            @NotBlank @Size(max = 150) String label,
            @NotBlank @Size(max = 80) String reasonCode,
            @NotBlank @Size(min = 5, max = 1000) String message,
            @Size(max = 1000) String editPath
    ) {
    }

    public record RequestChangesRequest(
            @NotNull Long version,
            @NotEmpty List<ChangeRequestItemRequest> items,
            @NotBlank @Size(min = 10, max = 1000) String applicantMessage
    ) {
    }

    public record ActionResponse(
            Long applicationId,
            String approvalStatus,
            Long storeId,
            Long version,
            OffsetDateTime reviewedAt
    ) {
    }

    public record DocumentAccessRequest(@NotBlank String purpose) {
    }

    public record DocumentAccessResponse(
            String accessUrl,
            OffsetDateTime expiresAt
    ) {
    }

    public record ChangeRequestItemResponse(
            Long id,
            String field,
            String label,
            String reasonCode,
            String message,
            String editPath,
            Integer displayOrder
    ) {
    }

    public record HistoryItemResponse(
            Long reviewId,
            String previousStatus,
            String nextStatus,
            String reasonCode,
            String reason,
            String comment,
            Integer reviewedBy,
            OffsetDateTime reviewedAt,
            String requestId,
            Long changeRequestId,
            String changeRequestStatus,
            String applicantMessage,
            List<ChangeRequestItemResponse> items
    ) {
    }

    public record HistoryResponse(List<HistoryItemResponse> content) {
    }
}
