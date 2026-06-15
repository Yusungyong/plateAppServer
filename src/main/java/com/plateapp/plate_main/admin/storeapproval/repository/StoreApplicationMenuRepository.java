package com.plateapp.plate_main.admin.storeapproval.repository;

import com.plateapp.plate_main.admin.storeapproval.entity.StoreApplicationMenu;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreApplicationMenuRepository extends JpaRepository<StoreApplicationMenu, Long> {

    List<StoreApplicationMenu> findByApplicationIdOrderByDisplayOrderAscIdAsc(Long applicationId);
}
