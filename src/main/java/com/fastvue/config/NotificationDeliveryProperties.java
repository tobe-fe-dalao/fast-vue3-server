package com.fastvue.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.notifications")
public record NotificationDeliveryProperties(
        @DefaultValue("20") @Min(1) @Max(100) int batchSize,
        @DefaultValue("10") @Min(1) @Max(100) int maxAttempts) {}
