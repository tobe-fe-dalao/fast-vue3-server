package com.fastvue.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.storage")
public record StorageProperties(
        @NotBlank String localRoot, @NotBlank String publicBaseUrl, @Min(1) long maxSizeBytes) {}
