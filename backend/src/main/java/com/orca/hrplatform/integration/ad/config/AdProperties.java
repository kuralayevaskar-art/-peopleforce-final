package com.orca.hrplatform.integration.ad.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "app.ad")
public class AdProperties {
    private boolean enabled;
    private String url;
    private String baseDn;
    private String bindDn;
    private String bindPassword;
    private String bindDomain = "dmuk.edu.kz";
    private String userFilter = "(sAMAccountName={0})";
    private String groupFilter = "(&(objectClass=group)(member={0}))";
    private String defaultRoleCode = "HR_MANAGER";
    private List<String> searchBaseDns = new ArrayList<>();
}
