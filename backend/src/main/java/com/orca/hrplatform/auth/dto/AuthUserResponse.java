package com.orca.hrplatform.auth.dto;

import lombok.Builder;
import lombok.Data;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
public class AuthUserResponse {
    private UUID id;
    private UUID companyId;
    private UUID employeeId;
    private String email;
    private String status;
    private Set<String> roles;
    private String fullName;
}
