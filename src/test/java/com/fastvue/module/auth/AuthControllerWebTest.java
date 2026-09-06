package com.fastvue.module.auth;

import com.fastvue.module.user.CreateUserRequest;
import com.fastvue.module.user.UserService;
import com.fastvue.module.user.UserVO;
import com.fastvue.security.JwtAuthenticationFilter;
import com.fastvue.security.JwtTokenProvider;
import com.fastvue.security.RefreshTokenStore;
import com.fastvue.security.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 认证公开接口的 MVC 契约测试。 */
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AuthControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private RefreshTokenStore refreshTokenStore;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("未登录用户可以注册")
    void registerIsPublic() throws Exception {
        when(userService.create(any(CreateUserRequest.class))).thenReturn(new UserVO(
                21L, "new-user", "new-user", "new@example.com", null,
                "active", List.of(), OffsetDateTime.now(), OffsetDateTime.now()));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content("{\"username\":\"new-user\",\"email\":\"new@example.com\",\"password\":\"123456\"}"))
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
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType("application/json")
                        .content("{\"username\":\"\",\"email\":\"invalid\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").isNumber())
                .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.not(0)));
    }
}
