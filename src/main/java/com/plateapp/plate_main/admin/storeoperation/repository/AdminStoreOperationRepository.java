package com.plateapp.plate_main.admin.storeoperation.repository;

import com.plateapp.plate_main.admin.storeoperation.entity.AdminStoreOperation;
import com.plateapp.plate_main.restaurant.entity.Restaurant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminStoreOperationRepository extends JpaRepository<AdminStoreOperation, Long> {
    @Query("""
        select r from Restaurant r
        left join AdminStoreOperation s on s.storeId = r.id
        where (:keyword is null or lower(r.title) like lower(concat('%', :keyword, '%'))
               or lower(r.address) like lower(concat('%', :keyword, '%')))
          and (:operationStatus is null or coalesce(s.operationStatus, 'operating') = :operationStatus)
          and (:visibilityStatus is null or
               case when s.storeId is null
                    then case when lower(r.exposureStatus) in ('published', 'visible') then 'visible' else 'hidden' end
                    else s.visibilityStatus end = :visibilityStatus)
        """)
    Page<Restaurant> searchStores(
            @Param("keyword") String keyword,
            @Param("operationStatus") String operationStatus,
            @Param("visibilityStatus") String visibilityStatus,
            Pageable pageable
    );
}
