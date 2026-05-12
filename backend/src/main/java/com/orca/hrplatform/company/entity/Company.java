package com.orca.hrplatform.company.entity;

import com.orca.hrplatform.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "companies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Company extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String bin;

    private String email;
    private String phone;
    private String address;
    private String logoFileId;

    @Builder.Default
    private String timezone = "Asia/Almaty";

    @Builder.Default
    private String defaultLanguage = "ru";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompanyStatus status;
}
