package com.plateapp.plate_main.admin.contentverification.repository;
import com.plateapp.plate_main.admin.contentverification.entity.ContentVerification;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface ContentVerificationRepository extends JpaRepository<ContentVerification,Long> {
 @Query("""
 select v from ContentVerification v where (:status is null or v.status=:status)
 and (:targetType is null or v.targetType=:targetType)
 and (:assignee is null or v.assigneeUserId=:assignee)
 and (:keyword is null or lower(v.targetId) like lower(concat('%',:keyword,'%'))
 or lower(coalesce(v.reviewReason,'')) like lower(concat('%',:keyword,'%')))""")
 Page<ContentVerification> search(@Param("status") String status,@Param("targetType") String targetType,
   @Param("assignee") Integer assignee,@Param("keyword") String keyword,Pageable pageable);
}
