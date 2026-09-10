package com.fastvue.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;
import org.springdoc.core.models.GroupedOpenApi;

import java.util.LinkedHashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiDocumentationCustomizerTest {

    @Test
    void createsThreeNamedLanguageGroups() {
        OpenApiConfig config = new OpenApiConfig();

        GroupedOpenApi chinese = config.chineseOpenApi();
        GroupedOpenApi japanese = config.japaneseOpenApi();
        GroupedOpenApi english = config.englishOpenApi();

        assertThat(chinese.getGroup()).isEqualTo("zh-CN");
        assertThat(chinese.getDisplayName()).isEqualTo("中文");
        assertThat(japanese.getGroup()).isEqualTo("ja-JP");
        assertThat(japanese.getDisplayName()).isEqualTo("日本語");
        assertThat(english.getGroup()).isEqualTo("en-US");
        assertThat(english.getDisplayName()).isEqualTo("English");
        assertThat(chinese.getPathsToMatch()).containsExactly("/api/**");
    }

    @Test
    void localizesPublicOperationToJapaneseWithoutAuthenticationRequirement() {
        Operation login = operationWithSuccessResponse()
                .parameters(List.of(new Parameter().name("tenantCode").in("query")));
        OpenAPI openApi = document("/api/v1/auth/login", PathItem.HttpMethod.POST, login);

        new OpenApiDocumentationCustomizer(OpenApiDocumentationCustomizer.Language.JA_JP).customise(openApi);

        assertThat(openApi.getInfo().getTitle()).isEqualTo("fast-vue3-server API ドキュメント");
        assertThat(login.getSummary()).isEqualTo("ログイン");
        assertThat(login.getDescription()).contains("ログイン不要");
        assertThat(login.getTags()).containsExactly("認証");
        assertThat(login.getSecurity()).isEmpty();
        assertThat(login.getResponses()).containsKeys("200", "400", "429", "500");
        assertThat(login.getResponses()).doesNotContainKey("401");
        assertThat(login.getResponses().get("400").getDescription()).isEqualTo("リクエストパラメータが不正です");
        assertThat(login.getParameters().getFirst().getDescription()).contains("テナントコード");
    }

    @Test
    void documentsProtectedOperationAndSchemaFieldsInEnglish() {
        Operation getUser = operationWithSuccessResponse()
                .parameters(List.of(new Parameter().name("id").in("path")));
        OpenAPI openApi = document("/api/v1/users/{id}", PathItem.HttpMethod.GET, getUser);
        ObjectSchema userSchema = new ObjectSchema();
        userSchema.setProperties(new LinkedHashMap<>());
        userSchema.addProperty("username", new StringSchema());
        openApi.getComponents().addSchemas("UserVO", userSchema);

        new OpenApiDocumentationCustomizer(OpenApiDocumentationCustomizer.Language.EN_US).customise(openApi);

        assertThat(getUser.getSummary()).isEqualTo("Get user details");
        assertThat(getUser.getDescription()).contains("Bearer Access Token");
        assertThat(getUser.getTags()).containsExactly("Users");
        assertThat(getUser.getSecurity()).singleElement()
                .satisfies(requirement -> assertThat(requirement).containsKey(OpenApiConfig.SECURITY_SCHEME_NAME));
        assertThat(getUser.getResponses()).containsKeys("401", "403", "404");
        assertThat(getUser.getParameters().getFirst().getDescription()).isEqualTo("Resource ID");
        assertThat(userSchema.getDescription()).isEqualTo("API data model");
        assertThat(userSchema.getProperties().get("username").getDescription()).isEqualTo("Username");
    }

    private OpenAPI document(String path, PathItem.HttpMethod method, Operation operation) {
        PathItem pathItem = new PathItem();
        pathItem.operation(method, operation);
        return new OpenAPI()
                .components(new Components().addSecuritySchemes(
                        OpenApiConfig.SECURITY_SCHEME_NAME,
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer")))
                .paths(new Paths().addPathItem(path, pathItem));
    }

    private Operation operationWithSuccessResponse() {
        return new Operation().responses(new ApiResponses()
                .addApiResponse("200", new ApiResponse().description("OK")));
    }
}
