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
    private String powershellExecutable = "powershell.exe";
    private String adTargetOu;
    private String defaultDepartment = "DMUK";
    private boolean changePasswordAtLogon = false;
    private boolean m365SyncEnabled = true;
    private String m365SyncHost;
    private String m365SyncUsername;
    private String m365SyncPassword;
}
