package com.fastvue.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.database")
public record DatabaseTimeoutProperties(
        @Min(1) @Max(300) int statementTimeoutSeconds,
        @Min(1) @Max(600) int transactionTimeoutSeconds,
        @Min(1) @Max(300000) int lockTimeoutMillis) {
    @AssertTrue(
            message =
                    "lock timeout must not exceed statement timeout, which must not exceed transaction timeout")
    public boolean isTimeoutOrderValid() {
        return lockTimeoutMillis <= statementTimeoutSeconds * 1000L
                && statementTimeoutSeconds <= transactionTimeoutSeconds;
    }
}
