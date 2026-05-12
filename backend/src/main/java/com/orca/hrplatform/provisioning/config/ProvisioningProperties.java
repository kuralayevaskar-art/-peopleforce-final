package com.orca.hrplatform.provisioning.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.provisioning")
public class ProvisioningProperties {
    private String defaultDomain;
    private boolean dryRun = true;
}
