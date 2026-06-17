package com.plateapp.plate_main.admin.storeapproval.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Getter;

@Entity
@Table(name = "store_applications")
@Getter
public class StoreApplication {

    public static final String STATUS_DRAFT = "draft";
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_ON_HOLD = "on_hold";
    public static final String STATUS_APPROVED = "approved";
    public static final String STATUS_REJECTED = "rejected";
    public static final String VERIFICATION_NOT_REQUESTED = "not_requested";
    public static final String VERIFICATION_REVIEWING = "reviewing";
    public static final String VERIFICATION_VERIFIED = "verified";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "parent_application_id")
    private Long parentApplicationId;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "applicant_user_id", nullable = false)
    private Integer applicantUserId;

    @Column(name = "store_name", nullable = false, length = 150)
    private String storeName;

    @Column(name = "region_code", nullable = false, length = 50)
    private String regionCode;

    @Column(name = "address", nullable = false, length = 300)
    private String address;

    @Column(name = "phone", length = 40)
    private String phone;

    @Column(name = "email", length = 320)
    private String email;

    @Column(name = "owner_name", nullable = false, length = 100)
    private String ownerName;

    @Column(name = "business_number_encrypted", nullable = false)
    private byte[] businessNumberEncrypted;

    @Column(name = "business_number_hash", nullable = false, length = 128)
    private String businessNumberHash;

    @Column(name = "business_name", length = 150)
    private String businessName;

    @Column(name = "business_representative_name", length = 100)
    private String businessRepresentativeName;

    @Column(name = "business_opening_date")
    private LocalDate businessOpeningDate;

    @Column(name = "business_verification_provider", length = 30)
    private String businessVerificationProvider;

    @Column(name = "business_verification_status", length = 30)
    private String businessVerificationStatus;

    @Column(name = "business_verified_at")
    private OffsetDateTime businessVerifiedAt;

    @Column(name = "business_verification_message", length = 300)
    private String businessVerificationMessage;

    @Column(name = "approval_status", nullable = false, length = 30)
    private String approvalStatus;

    @Column(name = "verification_status", nullable = false, length = 30)
    private String verificationStatus;

    @Column(name = "main_image_object_key")
    private String mainImageObjectKey;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "applied_at", nullable = false)
    private OffsetDateTime appliedAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    @Column(name = "reviewed_by")
    private Integer reviewedBy;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected StoreApplication() {
    }

    public static StoreApplication createDraft(
            Long parentApplicationId,
            Integer applicantUserId,
            String storeName,
            String regionCode,
            String address,
            String phone,
            String email,
            String ownerName,
            byte[] businessNumberEncrypted,
            String businessNumberHash,
            String businessName,
            String description,
            OffsetDateTime now
    ) {
        StoreApplication application = new StoreApplication();
        application.parentApplicationId = parentApplicationId;
        application.applicantUserId = applicantUserId;
        application.businessNumberEncrypted = businessNumberEncrypted;
        application.businessNumberHash = businessNumberHash;
        application.businessName = businessName;
        application.approvalStatus = STATUS_DRAFT;
        application.verificationStatus = VERIFICATION_NOT_REQUESTED;
        application.appliedAt = now;
        application.updatedAt = now;
        application.updateDraft(storeName, regionCode, address, phone, email, ownerName, description, now);
        return application;
    }

    public void updateDraft(
            String storeName,
            String regionCode,
            String address,
            String phone,
            String email,
            String ownerName,
            String description,
            OffsetDateTime now
    ) {
        this.storeName = storeName;
        this.regionCode = regionCode;
        this.address = address;
        this.phone = phone;
        this.email = email;
        this.ownerName = ownerName;
        this.description = description;
        this.updatedAt = now;
    }

    public void replaceBusinessNumber(byte[] businessNumberEncrypted, String businessNumberHash, OffsetDateTime now) {
        this.businessNumberEncrypted = businessNumberEncrypted;
        this.businessNumberHash = businessNumberHash;
        this.updatedAt = now;
    }

    public void updateBusinessName(String businessName, OffsetDateTime now) {
        this.businessName = businessName;
        this.updatedAt = now;
    }

    public void updateBusinessVerification(
            String businessRepresentativeName,
            LocalDate businessOpeningDate,
            String businessVerificationProvider,
            String businessVerificationStatus,
            OffsetDateTime businessVerifiedAt,
            String businessVerificationMessage,
            OffsetDateTime now
    ) {
        this.businessRepresentativeName = businessRepresentativeName;
        this.businessOpeningDate = businessOpeningDate;
        this.businessVerificationProvider = businessVerificationProvider;
        this.businessVerificationStatus = businessVerificationStatus;
        this.businessVerifiedAt = businessVerifiedAt;
        this.businessVerificationMessage = businessVerificationMessage;
        this.updatedAt = now;
    }

    public void submit(OffsetDateTime now) {
        this.approvalStatus = STATUS_PENDING;
        this.verificationStatus = VERIFICATION_REVIEWING;
        this.updatedAt = now;
    }

    public void approve(Long createdStoreId, Integer actorUserId, OffsetDateTime now) {
        this.approvalStatus = STATUS_APPROVED;
        this.storeId = createdStoreId;
        markReviewed(actorUserId, now);
    }

    public void hold(Integer actorUserId, OffsetDateTime now) {
        this.approvalStatus = STATUS_ON_HOLD;
        markReviewed(actorUserId, now);
    }

    public void reject(Integer actorUserId, OffsetDateTime now) {
        this.approvalStatus = STATUS_REJECTED;
        markReviewed(actorUserId, now);
    }

    private void markReviewed(Integer actorUserId, OffsetDateTime now) {
        this.reviewedBy = actorUserId;
        this.reviewedAt = now;
        this.updatedAt = now;
    }
}
