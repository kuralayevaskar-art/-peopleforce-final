package com.orca.hrplatform.mail.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.util.StringUtils;

import java.util.Properties;

@Configuration
@RequiredArgsConstructor
public class MailConfig {
    private final MailProperties properties;

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(properties.getHost());
        sender.setPort(properties.getPort());
        sender.setUsername(properties.getUsername());
        sender.setPassword(properties.getPassword());

        Properties javaMailProperties = sender.getJavaMailProperties();
        javaMailProperties.put("mail.transport.protocol", "smtp");
        javaMailProperties.put("mail.smtp.auth", String.valueOf(StringUtils.hasText(properties.getUsername())));
        javaMailProperties.put("mail.smtp.starttls.enable", String.valueOf(properties.isStartTls()));
        javaMailProperties.put("mail.smtp.starttls.required", String.valueOf(properties.isStartTls()));
        javaMailProperties.put("mail.smtp.ssl.trust", properties.getHost());
        javaMailProperties.put("mail.smtp.connectiontimeout", "15000");
        javaMailProperties.put("mail.smtp.timeout", "15000");
        javaMailProperties.put("mail.smtp.writetimeout", "15000");
        javaMailProperties.put("mail.smtp.auth.mechanisms", "LOGIN PLAIN");
        return sender;
    }
}
