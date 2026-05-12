package com.orca.hrplatform.position.entity;

import com.orca.hrplatform.common.entity.BaseEntity;
import com.orca.hrplatform.common.entity.DirectoryStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "positions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Position extends BaseEntity {

    @Column(nullable = false)
    private UUID companyId;

    private UUID departmentId;

    @Column(nullable = false)
    private String name;

    private String description;
    private String level;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DirectoryStatus status;
}
