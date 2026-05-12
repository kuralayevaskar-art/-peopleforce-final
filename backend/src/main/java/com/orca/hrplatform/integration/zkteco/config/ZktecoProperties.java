package com.orca.hrplatform.integration.zkteco.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.zkteco")
public class ZktecoProperties {
    private boolean enabled;
    private String host;
    private int port = 5442;
    private String databaseName;
    private String username;
    private String password;
    private String photoRoot;
    private int importBatchSize = 500;

    public String jdbcUrl() {
        return "jdbc:postgresql://" + host + ":" + port + "/" + databaseName + "?sslmode=disable";
    }
}
