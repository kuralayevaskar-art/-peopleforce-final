package com.orca.hrplatform.integration.ad.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AdSettingsRequest {
    @NotBlank(message = "AD URL is required")
    private String url;

    @NotBlank(message = "Base DN is required")
    private String baseDn;

    private List<String> searchBaseDns = new ArrayList<>();

    @NotBlank(message = "Bind DN is required")
    private String bindDn;

    private String bindPassword;

    @NotBlank(message = "User filter is required")
    private String userFilter;

    private String groupFilter;
}
