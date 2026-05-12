package com.orca.hrplatform.integration.zkteco.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ZktecoSettingsResponse {
    private boolean enabled;
    private String host;
    private String databaseName;
    private boolean usernameConfigured;
    private boolean passwordConfigured;
}
