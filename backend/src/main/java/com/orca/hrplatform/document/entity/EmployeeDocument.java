package com.orca.hrplatform.document.entity;

import com.orca.hrplatform.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "employee_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeDocument extends BaseEntity {

    @Column(nullable = false)
    private UUID companyId;

    @Column(nullable = false)
    private UUID employeeId;

    @Column(nullable = false)
    private UUID documentTypeId;

    @Column(nullable = false)
    private UUID fileId;

    @Column(nullable = false)
    private String title;

    private LocalDate issueDate;
    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentStatus status;

    private UUID uploadedBy;
}
