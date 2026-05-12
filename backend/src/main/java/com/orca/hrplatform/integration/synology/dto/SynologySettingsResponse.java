package com.orca.hrplatform.integration.synology.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SynologySettingsResponse {
    private boolean enabled;
    private String host;
    private String rootPath;
    private boolean usernameConfigured;
    private boolean passwordConfigured;
}
