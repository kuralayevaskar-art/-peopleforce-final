package com.orca.hrplatform.audit.service;

import com.orca.hrplatform.audit.entity.AuditLog;
import com.orca.hrplatform.audit.repository.AuditLogRepository;
import com.orca.hrplatform.auth.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {
    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void success(User actor, String action, UUID targetUserId, UUID targetEmployeeId, String oldValue, String newValue) {
        write(actor, action, "SUCCESS", targetUserId, targetEmployeeId, oldValue, newValue, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failure(User actor, String action, UUID targetUserId, UUID targetEmployeeId, String errorMessage) {
        write(actor, action, "ERROR", targetUserId, targetEmployeeId, null, null, errorMessage);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void login(String login, boolean success, String message, String ipAddress) {
        if (success) {
            log.info("AUDIT action=LOGIN status=SUCCESS actor={} ip={}", login, ipAddress);
        } else {
            log.warn("AUDIT action=LOGIN status=ERROR actor={} ip={} error={}", login, ipAddress, safe(message));
        }
        auditLogRepository.save(AuditLog.builder()
                .actorEmail(login)
                .action("LOGIN")
                .status(success ? "SUCCESS" : "ERROR")
                .errorMessage(success ? null : message)
                .ipAddress(ipAddress)
                .build());
    }

    private void write(User actor, String action, String status, UUID targetUserId, UUID targetEmployeeId,
                       String oldValue, String newValue, String errorMessage) {
        String actorEmail = actor != null ? actor.getEmail() : null;
        if ("ERROR".equals(status)) {
            log.warn("AUDIT action={} status={} actor={} targetUserId={} targetEmployeeId={} error={}",
                    action, status, actorEmail, targetUserId, targetEmployeeId, safe(errorMessage));
        } else {
            log.info("AUDIT action={} status={} actor={} targetUserId={} targetEmployeeId={} newValue={}",
                    action, status, actorEmail, targetUserId, targetEmployeeId, safe(newValue));
        }
        auditLogRepository.save(AuditLog.builder()
                .companyId(actor != null ? actor.getCompanyId() : null)
                .actorUserId(actor != null ? actor.getId() : null)
                .actorEmail(actor != null ? actor.getEmail() : null)
                .targetUserId(targetUserId)
                .targetEmployeeId(targetEmployeeId)
                .action(action)
                .status(status)
                .oldValue(oldValue)
                .newValue(newValue)
                .errorMessage(errorMessage)
                .build());
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replaceAll("(?i)password\\s*[:=]\\s*\\S+", "password=***")
                .replaceAll("(?i)(MAIL_PASSWORD|M365_SYNC_PASSWORD|AD_BIND_PASSWORD)=\\S+", "$1=***");
    }
}
