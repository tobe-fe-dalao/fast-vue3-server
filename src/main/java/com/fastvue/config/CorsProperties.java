package com.fastvue.config;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.cors")
public record CorsProperties(@NotEmpty List<String> allowedOriginPatterns) {
    public CorsProperties {
        if (allowedOriginPatterns != null)
            allowedOriginPatterns = List.copyOf(allowedOriginPatterns);
    }
}
