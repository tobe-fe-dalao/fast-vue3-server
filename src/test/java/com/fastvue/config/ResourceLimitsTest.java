package com.fastvue.config;

import static org.assertj.core.api.Assertions.*;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.util.unit.DataSize;

class ResourceLimitsTest {
    @Test
    void invalidDatabaseBudgetsFailValidation() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertThat(validator.validate(new DatabaseTimeoutProperties(15, 20, 5000))).isEmpty();
            assertThat(validator.validate(new DatabaseTimeoutProperties(0, 20, 5000))).isNotEmpty();
            assertThat(validator.validate(new DatabaseTimeoutProperties(15, 10, 5000)))
                    .isNotEmpty();
            assertThat(validator.validate(new DatabaseTimeoutProperties(15, 20, 16000)))
                    .isNotEmpty();
        }
    }

    @Test
    void uploadBudgetsAllowOverheadAndRejectDivergentLimits() throws Exception {
        var validator = new UploadConfigurationValidator();
        var storage = new StorageProperties("/tmp", "/files", 1024 * 1024);
        var multipart = new MultipartProperties();
        multipart.setMaxFileSize(DataSize.ofMegabytes(1));
        multipart.setMaxRequestSize(DataSize.ofMegabytes(2));
        validator.validateUploadLimits(storage, multipart).afterPropertiesSet();
        multipart.setMaxRequestSize(DataSize.ofMegabytes(1));
        assertThatThrownBy(
                        () ->
                                validator
                                        .validateUploadLimits(storage, multipart)
                                        .afterPropertiesSet())
                .hasMessageContaining("overhead");
        multipart.setMaxFileSize(DataSize.ofMegabytes(2));
        assertThatThrownBy(
                        () ->
                                validator
                                        .validateUploadLimits(storage, multipart)
                                        .afterPropertiesSet())
                .hasMessageContaining("must match");
    }
}
