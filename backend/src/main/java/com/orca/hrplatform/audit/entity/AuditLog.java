package com.orca.hrplatform.audit.entity;

import com.orca.hrplatform.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog extends BaseEntity {
    private UUID companyId;
    private UUID actorUserId;
    private String actorEmail;
    private UUID targetUserId;
    private UUID targetEmployeeId;
    @Column(nullable = false)
    private String action;
    @Column(nullable = false)
    private String status;
    @Column(columnDefinition = "TEXT")
    private String oldValue;
    @Column(columnDefinition = "TEXT")
    private String newValue;
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
    private String ipAddress;
}
