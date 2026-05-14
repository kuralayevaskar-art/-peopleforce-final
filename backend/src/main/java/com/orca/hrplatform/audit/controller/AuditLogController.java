package com.orca.hrplatform.audit.controller;

import com.orca.hrplatform.audit.entity.AuditLog;
import com.orca.hrplatform.audit.repository.AuditLogRepository;
import com.orca.hrplatform.auth.entity.User;
import com.orca.hrplatform.auth.repository.UserRepository;
import com.orca.hrplatform.common.response.ApiResponse;
import com.orca.hrplatform.common.security.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ApiResponse<List<AuditLog>> list() {
        User current = userRepository.findByEmail(SecurityUtils.getCurrentUserEmail())
                .orElseThrow(() -> new EntityNotFoundException("Current user not found"));
        return ApiResponse.success(auditLogRepository.findTop100ByCompanyIdOrderByCreatedAtDesc(current.getCompanyId()));
    }
}
