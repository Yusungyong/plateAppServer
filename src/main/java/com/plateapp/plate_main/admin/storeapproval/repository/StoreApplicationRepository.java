package com.plateapp.plate_main.admin.storeapproval.repository;

import com.plateapp.plate_main.admin.storeapproval.entity.StoreApplication;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoreApplicationRepository extends JpaRepository<StoreApplication, Long> {

    Page<StoreApplication> findByApplicantUserId(Integer applicantUserId, Pageable pageable);

    Optional<StoreApplication> findByIdAndApplicantUserId(Long id, Integer applicantUserId);

    Optional<StoreApplication> findFirstByApplicantUserIdAndBusinessNumberHashAndApprovalStatusOrderByAppliedAtDescIdDesc(
            Integer applicantUserId,
            String businessNumberHash,
            String approvalStatus
    );

    boolean existsByBusinessNumberHashAndApprovalStatusIn(
            String businessNumberHash,
            Collection<String> approvalStatuses
    );

    boolean existsByBusinessNumberHashAndApprovalStatusInAndIdNot(
            String businessNumberHash,
            Collection<String> approvalStatuses,
            Long id
    );

    @Query("""
        select application
        from StoreApplication application
        where (:hasKeyword = false
               or lower(application.storeName) like :keywordPattern
               or lower(application.ownerName) like :keywordPattern
               or lower(coalesce(application.phone, '')) like :keywordPattern
               or lower(application.address) like :keywordPattern
               or application.businessNumberHash = :businessNumberHash)
          and (:region is null or application.regionCode = :region)
          and (:status is null or application.approvalStatus = :status)
          and (:verificationStatus is null or application.verificationStatus = :verificationStatus)
          and (:hasAppliedFrom = false or application.appliedAt >= :appliedFrom)
          and (:hasAppliedTo = false or application.appliedAt < :appliedToExclusive)
          and (:category is null or exists (
              select 1
              from StoreApplicationCategory category
              where category.applicationId = application.id
                and category.categoryCode = :category
          ))
        """)
    Page<StoreApplication> search(
            @Param("hasKeyword") boolean hasKeyword,
            @Param("keywordPattern") String keywordPattern,
            @Param("businessNumberHash") String businessNumberHash,
            @Param("region") String region,
            @Param("category") String category,
            @Param("status") String status,
            @Param("verificationStatus") String verificationStatus,
            @Param("hasAppliedFrom") boolean hasAppliedFrom,
            @Param("appliedFrom") OffsetDateTime appliedFrom,
            @Param("hasAppliedTo") boolean hasAppliedTo,
            @Param("appliedToExclusive") OffsetDateTime appliedToExclusive,
            Pageable pageable
    );

    boolean existsByBusinessNumberHashAndApprovalStatusAndIdNot(
            String businessNumberHash,
            String approvalStatus,
            Long id
    );
}
