package com.plateapp.plate_main.auth.repository;

import com.plateapp.plate_main.auth.domain.AdminUserPermission;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminUserPermissionRepository extends JpaRepository<AdminUserPermission, Long> {

    boolean existsByUserId(Integer userId);

    List<AdminUserPermission> findByUserIdAndRevokedAtIsNullOrderByPermissionAsc(Integer userId);
}
