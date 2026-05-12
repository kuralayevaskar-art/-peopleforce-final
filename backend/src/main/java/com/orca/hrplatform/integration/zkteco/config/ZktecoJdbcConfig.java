package com.orca.hrplatform.integration.zkteco.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@Configuration
public class ZktecoJdbcConfig {

    @Bean
    @ConditionalOnProperty(prefix = "app.zkteco", name = "enabled", havingValue = "true")
    public JdbcTemplate zktecoJdbcTemplate(ZktecoProperties properties) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl(properties.jdbcUrl());
        dataSource.setUsername(properties.getUsername());
        dataSource.setPassword(properties.getPassword());
        return new JdbcTemplate(dataSource);
    }
}
