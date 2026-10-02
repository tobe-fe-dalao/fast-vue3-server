package com.fastvue.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
    AdminProperties.class,
    StorageProperties.class,
    DatabaseTimeoutProperties.class
})
public class InfrastructurePropertiesConfig {}
