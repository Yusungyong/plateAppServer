package com.plateapp.plate_main.admin.audit.repository;

import com.plateapp.plate_main.admin.audit.entity.AdminAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {
}
