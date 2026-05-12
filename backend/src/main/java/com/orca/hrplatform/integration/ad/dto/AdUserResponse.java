package com.orca.hrplatform.integration.ad.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdUserResponse {
    private String username;
    private String displayName;
    private String email;
    private String department;
    private String title;
    private String manager;
    private String dn;
}
