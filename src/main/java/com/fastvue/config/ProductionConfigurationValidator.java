package com.fastvue.config;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
@RequiredArgsConstructor
public class ProductionConfigurationValidator {
    private final JwtProperties jwt;
    private final CorsProperties cors;
    private final Environment environment;

    @PostConstruct
    public void validate() {
        if (environment.acceptsProfiles(org.springframework.core.env.Profiles.of("dev"))
                || jwt.secret().startsWith("change-me")
                || environment.getProperty("app.demo.enabled", Boolean.class, false)) {
            throw new IllegalStateException(
                    "Production cannot use development secrets, profiles or demo adapters");
        }
        String password = environment.getRequiredProperty("app.admin.password");
        if (password.length() < 12 || password.equals("admin123")) {
            throw new IllegalStateException(
                    "Production bootstrap password must contain at least 12 characters");
        }
        for (String origin : cors.allowedOriginPatterns()) {
            URI uri = URI.create(origin);
            if (!"https".equals(uri.getScheme())
                    || uri.getHost() == null
                    || origin.contains("*")
                    || uri.getUserInfo() != null
                    || uri.getQuery() != null
                    || uri.getFragment() != null
                    || uri.getPath() != null && !uri.getPath().isEmpty()) {
                throw new IllegalStateException(
                        "Production CORS requires explicit HTTPS origins without paths");
            }
        }
    }
}
