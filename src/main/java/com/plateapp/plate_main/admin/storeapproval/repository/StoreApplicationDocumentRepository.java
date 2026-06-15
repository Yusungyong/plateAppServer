package com.plateapp.plate_main.admin.storeapproval.repository;

import com.plateapp.plate_main.admin.storeapproval.entity.StoreApplicationDocument;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreApplicationDocumentRepository extends JpaRepository<StoreApplicationDocument, Long> {

    List<StoreApplicationDocument> findByApplicationIdOrderByCreatedAtAscIdAsc(Long applicationId);

    Optional<StoreApplicationDocument> findByIdAndApplicationId(Long id, Long applicationId);

    long countByApplicationId(Long applicationId);

    long countByApplicationIdAndVerificationStatus(Long applicationId, String verificationStatus);
}
