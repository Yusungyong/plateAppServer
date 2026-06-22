package com.plateapp.plate_main.owner.repository;

import com.plateapp.plate_main.owner.entity.StoreOwner;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreOwnerRepository extends JpaRepository<StoreOwner, Long> {

    boolean existsByUserIdAndRevokedAtIsNull(Integer userId);

    boolean existsByStoreIdAndUserIdAndRevokedAtIsNull(Long storeId, Integer userId);

    List<StoreOwner> findByUserIdAndRevokedAtIsNullOrderByCreatedAtDescIdDesc(Integer userId);

    List<StoreOwner> findByStoreIdAndRevokedAtIsNull(Long storeId);

    Optional<StoreOwner> findFirstByStoreIdAndUserIdOrderByCreatedAtDescIdDesc(Long storeId, Integer userId);
}
