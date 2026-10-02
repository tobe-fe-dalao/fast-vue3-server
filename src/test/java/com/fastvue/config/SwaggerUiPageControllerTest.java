package com.fastvue.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SwaggerUiPageControllerTest {

    @Test
    void redirectsKnownSwaggerUrlsToLocalizedPage() {
        SwaggerUiPageController controller = new SwaggerUiPageController();

        assertThat(controller.redirectToLocalizedUi())
                .isEqualTo("redirect:/api-docs-ui/index.html");
    }
}
