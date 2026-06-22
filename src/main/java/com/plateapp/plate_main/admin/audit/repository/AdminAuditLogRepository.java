package com.plateapp.plate_main.admin.audit.repository;

import com.plateapp.plate_main.admin.audit.entity.AdminAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {
    List<AdminAuditLog> findByResourceTypeAndResourceIdOrderByOccurredAtDescIdDesc(
            String resourceType,
            String resourceId
    );
}
