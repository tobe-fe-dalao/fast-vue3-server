package com.fastvue.module.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fastvue.module.auth.api.AuthController;
import com.fastvue.module.auth.service.AuthService;
import com.fastvue.module.tenant.persistence.TenantMapper;
import com.fastvue.module.tenant.service.TenantService;
import com.fastvue.module.user.api.CreateUserRequest;
import com.fastvue.module.user.api.UserVO;
import com.fastvue.module.user.service.UserService;
import com.fastvue.security.JwtAuthenticationFilter;
import com.fastvue.security.JwtTokenProvider;
import com.fastvue.security.RefreshTokenStore;
import com.fastvue.security.SecurityConfig;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** 认证公开接口的 MVC 契约测试。 */
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AuthControllerWebTest extends com.fastvue.security.SecurityMvcTestSupport {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AuthService authService;

    @MockitoBean private UserService userService;

    @MockitoBean private TenantService tenantService;

    @MockitoBean private JwtTokenProvider jwtTokenProvider;

    @MockitoBean private TenantMapper tenantMapper;

    @MockitoBean private RefreshTokenStore refreshTokenStore;

    @MockitoBean private UserDetailsService userDetailsService;

    @Test
    @DisplayName("未登录用户可以注册")
    void registerIsPublic() throws Exception {
        when(tenantService.inTenant(any(), any()))
                .thenAnswer(
                        invocation ->
                                ((java.util.function.Supplier<?>) invocation.getArgument(1)).get());
        when(userService.create(any(CreateUserRequest.class)))
                .thenReturn(
                        new UserVO(
                                21L,
                                "new-user",
                                "new-user",
                                "new@example.com",
                                null,
                                "active",
                                List.of(),
                                OffsetDateTime.now(),
                                OffsetDateTime.now()));

        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType("application/json")
                                .content(
                                        "{\"username\":\"new-user\",\"email\":\"new@example.com\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(21))
                .andExpect(jsonPath("$.data.username").value("new-user"))
                .andExpect(jsonPath("$.data.status").value("active"));

        verify(userService).create(any(CreateUserRequest.class));
    }

    @Test
    @DisplayName("注册参数由服务端统一校验")
    void registerValidatesRequest() throws Exception {
        mockMvc.perform(
                        post("/api/v1/auth/register")
                                .contentType("application/json")
                                .content(
                                        "{\"username\":\"\",\"email\":\"invalid\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").isNumber())
                .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.not(0)));
    }
}
