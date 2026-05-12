package com.orca.hrplatform.integration.synology.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.synology")
public class SynologyProperties {
    private boolean enabled;
    private String host;
    private String rootPath;
    private String username;
    private String password;
}
