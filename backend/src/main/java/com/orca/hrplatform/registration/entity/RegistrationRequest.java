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
@Table(name = "registration_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistrationRequest extends BaseEntity {
    @Column(nullable = false)
    private UUID companyId;
    @Column(nullable = false)
    private UUID linkId;
    @Column(nullable = false)
    private String fullName;
    @Column(nullable = false)
    private String firstName;
    @Column(nullable = false)
    private String lastName;
    @Column(nullable = false)
    private String department;
    @Column(nullable = false)
    private String phone;
    @Column(nullable = false)
    private String personalEmail;
    @Column(nullable = false)
    private String identityDocumentFileId;
    @Column(nullable = false)
    private String facePhotoFileId;
    @Column(nullable = false)
    private String status;
    private String corporateEmail;
    @Column(columnDefinition = "TEXT")
    private String rejectionReason;
    private UUID approvedBy;
    private LocalDateTime approvedAt;
    private UUID rejectedBy;
    private LocalDateTime rejectedAt;
}
