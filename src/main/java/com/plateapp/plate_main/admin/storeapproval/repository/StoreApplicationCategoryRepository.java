package com.plateapp.plate_main.admin.storeapproval.repository;

import com.plateapp.plate_main.admin.storeapproval.entity.StoreApplicationCategory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreApplicationCategoryRepository extends JpaRepository<StoreApplicationCategory, Long> {

    List<StoreApplicationCategory> findByApplicationIdOrderByDisplayOrderAscIdAsc(Long applicationId);

    void deleteByApplicationId(Long applicationId);
}
