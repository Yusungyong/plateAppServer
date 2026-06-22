package com.plateapp.plate_main.admin.contentverification.repository;
import com.plateapp.plate_main.admin.contentverification.entity.ContentVerificationHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ContentVerificationHistoryRepository extends JpaRepository<ContentVerificationHistory,Long> {
 List<ContentVerificationHistory> findByVerificationIdOrderByCreatedAtDescIdDesc(Long verificationId);
}
