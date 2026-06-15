package com.plateapp.plate_main.admin.storeapproval.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;

@Entity
@Table(name = "store_application_documents")
@Getter
public class StoreApplicationDocument {

    public static final String STATUS_VERIFIED = "verified";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Column(name = "document_type", nullable = false, length = 50)
    private String documentType;

    @Column(name = "object_key", nullable = false)
    private String objectKey;

    @Column(name = "original_name", nullable = false)
    private String originalName;

    @Column(name = "mime_type", length = 120)
    private String mimeType;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "verification_status", nullable = false, length = 30)
    private String verificationStatus;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "purge_at")
    private OffsetDateTime purgeAt;

    protected StoreApplicationDocument() {
    }
}
