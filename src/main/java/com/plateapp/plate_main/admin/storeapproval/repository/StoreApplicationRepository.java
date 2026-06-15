package com.plateapp.plate_main.admin.storeapproval.repository;

import com.plateapp.plate_main.admin.storeapproval.entity.StoreApplication;
import java.time.OffsetDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoreApplicationRepository extends JpaRepository<StoreApplication, Long> {

    @Query("""
        select application
        from StoreApplication application
        where (:keyword is null
               or lower(application.storeName) like lower(concat('%', :keyword, '%'))
               or lower(application.ownerName) like lower(concat('%', :keyword, '%'))
               or lower(coalesce(application.phone, '')) like lower(concat('%', :keyword, '%'))
               or lower(application.address) like lower(concat('%', :keyword, '%'))
               or (:businessNumberHash is not null and application.businessNumberHash = :businessNumberHash))
          and (:region is null or application.regionCode = :region)
          and (:status is null or application.approvalStatus = :status)
          and (:verificationStatus is null or application.verificationStatus = :verificationStatus)
          and (:appliedFrom is null or application.appliedAt >= :appliedFrom)
          and (:appliedToExclusive is null or application.appliedAt < :appliedToExclusive)
          and (:category is null or exists (
              select 1
              from StoreApplicationCategory category
              where category.applicationId = application.id
                and category.categoryCode = :category
          ))
        """)
    Page<StoreApplication> search(
            @Param("keyword") String keyword,
            @Param("businessNumberHash") String businessNumberHash,
            @Param("region") String region,
            @Param("category") String category,
            @Param("status") String status,
            @Param("verificationStatus") String verificationStatus,
            @Param("appliedFrom") OffsetDateTime appliedFrom,
            @Param("appliedToExclusive") OffsetDateTime appliedToExclusive,
            Pageable pageable
    );

    boolean existsByBusinessNumberHashAndApprovalStatusAndIdNot(
            String businessNumberHash,
            String approvalStatus,
            Long id
    );
}
