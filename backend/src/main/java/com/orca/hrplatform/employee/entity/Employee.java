package com.orca.hrplatform.employee.entity;

import com.orca.hrplatform.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "employees")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employee extends BaseEntity {

    @Column(nullable = false)
    private UUID companyId;

    private UUID departmentId;
    private UUID positionId;
    private UUID managerId;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    private String middleName;

    private String fullName;

    @Column(nullable = false)
    private String email;

    private String phone;
    private LocalDate birthDate;
    private LocalDate hireDate;
    private LocalDate terminationDate;

    @Enumerated(EnumType.STRING)
    private EmploymentType employmentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmployeeStatus status;

    private String avatarFileId;

    private String adUsername;
    private String zktecoPin;
    private String zktecoPersonId;
}
