package com.orca.hrplatform.mail.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.mail")
public class MailProperties {
    private boolean enabled;
    private String host;
    private int port = 587;
    private String username;
    private String password;
    private String from;
    private boolean startTls = true;
}
