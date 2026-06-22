package com.plateapp.plate_main.admin.feedback.repository;

import com.plateapp.plate_main.admin.feedback.entity.ServiceFeedback;
import java.time.OffsetDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;

public interface ServiceFeedbackRepository extends JpaRepository<ServiceFeedback, Long> {
    @Query("""
        select f from ServiceFeedback f
        where (:keyword is null or lower(f.content) like lower(concat('%', :keyword, '%'))
               or lower(coalesce(f.contact, '')) like lower(concat('%', :keyword, '%')))
          and (:type is null or f.type = :type)
          and (:status is null or f.status = :status)
          and (:from is null or f.createdAt >= :from)
          and (:to is null or f.createdAt < :to)
        """)
    Page<ServiceFeedback> search(@Param("keyword") String keyword, @Param("type") String type,
            @Param("status") String status, @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to, Pageable pageable);

    long countByStatus(String status);

    @Modifying
    @Query("update ServiceFeedback f set f.contact = null, f.contactPurgeAt = null "
            + "where f.contact is not null and f.contactPurgeAt <= :now")
    int purgeExpiredContacts(@Param("now") OffsetDateTime now);
}
