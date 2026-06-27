package com.plateapp.plate_main.admin.storeapproval.repository;

import com.plateapp.plate_main.admin.storeapproval.entity.StoreApplicationReview;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreApplicationReviewRepository extends JpaRepository<StoreApplicationReview, Long> {

    List<StoreApplicationReview> findByApplicationIdOrderByReviewedAtDescIdDesc(Long applicationId);

    Optional<StoreApplicationReview> findFirstByApplicationIdOrderByReviewedAtDescIdDesc(Long applicationId);

    Optional<StoreApplicationReview> findFirstByApplicationIdAndNextStatusOrderByReviewedAtDescIdDesc(
            Long applicationId,
            String nextStatus
    );
}
