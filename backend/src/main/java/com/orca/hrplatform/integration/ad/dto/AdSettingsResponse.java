package com.orca.hrplatform.integration.ad.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AdSettingsResponse {
    private boolean enabled;
    private String url;
    private String baseDn;
    private String bindDn;
    private String userFilter;
    private String groupFilter;
    private List<String> searchBaseDns;
}
