package com.orca.hrplatform.document.entity;

import com.orca.hrplatform.common.entity.BaseEntity;
import com.orca.hrplatform.common.entity.DirectoryStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "document_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentType extends BaseEntity {

    @Column(nullable = false)
    private UUID companyId;

    @Column(nullable = false)
    private String name;

    private String description;

    @Builder.Default
    private boolean isRequired = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DirectoryStatus status;
}
