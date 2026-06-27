package com.plateapp.plate_main.admin.storeapproval.repository;

import com.plateapp.plate_main.admin.storeapproval.entity.StoreApplicationChangeRequestItem;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreApplicationChangeRequestItemRepository
        extends JpaRepository<StoreApplicationChangeRequestItem, Long> {

    List<StoreApplicationChangeRequestItem> findByChangeRequestIdInOrderByDisplayOrderAscIdAsc(
            Collection<Long> changeRequestIds
    );
}
