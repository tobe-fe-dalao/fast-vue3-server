package com.fastvue.config;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ProductionConfigurationValidatorTest {
    @Test
    void rejectsDevelopmentSecretsWildcardOriginsAndMixedProfiles() {
        var environment =
                new MockEnvironment().withProperty("app.admin.password", "long-bootstrap-password");
        var jwt =
                new JwtProperties(
                        "safe-test-key-of-more-than-thirty-two-bytes",
                        900,
                        86400,
                        "issuer",
                        "audience");
        assertThatThrownBy(
                        () ->
                                new ProductionConfigurationValidator(
                                                jwt,
                                                new CorsProperties(
                                                        List.of("https://*.example.com")),
                                                environment)
                                        .validate())
                .isInstanceOf(IllegalStateException.class);
        environment.setActiveProfiles("dev", "prod");
        assertThatThrownBy(
                        () ->
                                new ProductionConfigurationValidator(
                                                jwt,
                                                new CorsProperties(
                                                        List.of("https://app.example.com")),
                                                environment)
                                        .validate())
                .isInstanceOf(IllegalStateException.class);
    }
}
