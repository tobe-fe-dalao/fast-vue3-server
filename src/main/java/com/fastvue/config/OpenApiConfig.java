package com.fastvue.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger 配置。
 *
 * <p>声明 Bearer Token 安全方案，并提供中文、日文和英文三组 API 文档。</p>
 */
@Configuration
public class OpenApiConfig {

    static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("fast-vue3-server API")
                        .description("fast-vue3 官方参考后端接口文档；请在 Swagger UI 顶部切换语言。")
                        .version("0.1.0"))
                .components(new Components().addSecuritySchemes(
                        SECURITY_SCHEME_NAME,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("请输入登录接口返回的 Access Token，无需添加 Bearer 前缀。")));
    }

    @Bean
    public GroupedOpenApi chineseOpenApi() {
        return localizedGroup("zh-CN", "中文", OpenApiDocumentationCustomizer.Language.ZH_CN);
    }

    @Bean
    public GroupedOpenApi japaneseOpenApi() {
        return localizedGroup("ja-JP", "日本語", OpenApiDocumentationCustomizer.Language.JA_JP);
    }

    @Bean
    public GroupedOpenApi englishOpenApi() {
        return localizedGroup("en-US", "English", OpenApiDocumentationCustomizer.Language.EN_US);
    }

    private GroupedOpenApi localizedGroup(
            String group,
            String displayName,
            OpenApiDocumentationCustomizer.Language language) {
        return GroupedOpenApi.builder()
                .group(group)
                .displayName(displayName)
                .pathsToMatch("/api/**")
                .addOpenApiCustomizer(new OpenApiDocumentationCustomizer(language))
                .build();
    }
}
