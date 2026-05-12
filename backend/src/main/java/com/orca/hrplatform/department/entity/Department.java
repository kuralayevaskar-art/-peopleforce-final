package com.orca.hrplatform.department.entity;

import com.orca.hrplatform.common.entity.BaseEntity;
import com.orca.hrplatform.common.entity.DirectoryStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "departments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Department extends BaseEntity {

    @Column(nullable = false)
    private UUID companyId;

    private UUID parentId;

    @Column(nullable = false)
    private String name;

    private String description;
    private UUID managerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DirectoryStatus status;
}
