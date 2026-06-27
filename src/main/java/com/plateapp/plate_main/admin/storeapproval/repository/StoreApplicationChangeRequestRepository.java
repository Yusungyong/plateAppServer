package com.plateapp.plate_main.admin.storeapproval.repository;

import com.plateapp.plate_main.admin.storeapproval.entity.StoreApplicationChangeRequest;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreApplicationChangeRequestRepository extends JpaRepository<StoreApplicationChangeRequest, Long> {

    List<StoreApplicationChangeRequest> findByApplicationIdOrderByRequestedAtDescIdDesc(Long applicationId);

    List<StoreApplicationChangeRequest> findByApplicationIdAndStatusOrderByRequestedAtDescIdDesc(
            Long applicationId,
            String status
    );

    List<StoreApplicationChangeRequest> findByApplicationIdAndReviewIdIn(Long applicationId, Collection<Long> reviewIds);
}
