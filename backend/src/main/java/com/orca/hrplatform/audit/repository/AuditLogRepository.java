package com.orca.hrplatform.audit.repository;

import com.orca.hrplatform.audit.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    List<AuditLog> findTop100ByCompanyIdOrderByCreatedAtDesc(UUID companyId);
}
