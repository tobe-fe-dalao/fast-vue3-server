package com.fastvue.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Keeps the familiar Swagger URLs while serving the localized documentation UI.
 */
@Controller
public class SwaggerUiPageController {

    @GetMapping({"/swagger-ui.html", "/swagger-ui", "/swagger-ui/", "/swagger-ui/index.html"})
    public String redirectToLocalizedUi() {
        return "redirect:/api-docs-ui/index.html";
    }
}
