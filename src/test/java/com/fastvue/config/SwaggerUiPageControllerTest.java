package com.fastvue.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SwaggerUiPageControllerTest {

    @Test
    void redirectsKnownSwaggerUrlsToLocalizedPage() {
        SwaggerUiPageController controller = new SwaggerUiPageController();

        assertThat(controller.redirectToLocalizedUi())
                .isEqualTo("redirect:/api-docs-ui/index.html");
    }
}
