package com.orca.hrplatform.common.security;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class UserPrincipal {
    private final UUID userId;
    private final UUID companyId;
    private final UUID employeeId;
    private final String email;
}
