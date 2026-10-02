package com.fastvue.config;

import jakarta.validation.constraints.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.jwt")
public record JwtProperties(
        @NotBlank @Size(min = 32) String secret,
        @Min(60) @Max(86400) long accessTokenTtl,
        @Min(60) @Max(2592000) long refreshTokenTtl,
        @NotBlank String issuer,
        @NotBlank String audience) {}
