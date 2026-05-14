package com.orca.hrplatform.registration.entity;

import com.orca.hrplatform.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "registration_links")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistrationLink extends BaseEntity {
    @Column(nullable = false)
    private UUID companyId;
    @Column(nullable = false, unique = true)
    private String tokenHash;
    @Column(nullable = false)
    private String status;
    @Column(nullable = false)
    private LocalDateTime expiresAt;
    private LocalDateTime usedAt;
    private UUID createdBy;
}
