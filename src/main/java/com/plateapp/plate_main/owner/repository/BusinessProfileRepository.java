package com.plateapp.plate_main.owner.repository;

import com.plateapp.plate_main.owner.entity.BusinessProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessProfileRepository extends JpaRepository<BusinessProfile, Long> {

    Optional<BusinessProfile> findByUserId(Integer userId);
}
