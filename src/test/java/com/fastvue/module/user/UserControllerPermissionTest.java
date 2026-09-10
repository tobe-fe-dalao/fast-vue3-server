package com.fastvue.module.user;

import com.fastvue.common.exception.GlobalExceptionHandler;
import com.fastvue.module.user.api.UserController;
import com.fastvue.module.tenant.persistence.TenantMapper;
import com.fastvue.module.user.api.UserVO;
import com.fastvue.module.user.service.UserService;
import com.fastvue.security.JwtAuthenticationFilter;
import com.fastvue.security.JwtTokenProvider;
import com.fastvue.security.RefreshTokenStore;
import com.fastvue.security.SecurityConfig;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RBAC 权限判断切片测试。
 *
 * <p>验证 {@code @PreAuthorize("hasAuthority('user:list')")} 在拥有 / 缺少权限时的表现。
 * 通过显式 Import 安全配置与全局异常处理，使 401 / 403 被正确映射。</p>
 */
@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, JwtAuthenticationFilter.class})
class UserControllerPermissionTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private TenantMapper tenantMapper;

    @MockBean
    private RefreshTokenStore refreshTokenStore;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("拥有 user:list 权限可访问")
    @WithMockUser(authorities = "user:list")
    void listWithPermission() throws Exception {
        when(userService.page(any())).thenReturn(new Page<>(1, 20));
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("缺少 user:list 权限返回 403")
    @WithMockUser(authorities = "role:list")
    void listWithoutPermission() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("未登录返回 401")
    void listUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("拥有 user:create 权限可创建")
    @WithMockUser(authorities = "user:create")
    void createWithPermission() throws Exception {
        when(userService.create(any())).thenReturn(new UserVO(1L, "test", "测试", null, null, "active", List.of(), null, null));
        mockMvc.perform(post("/api/v1/users")
                        .contentType("application/json")
                        .content("{\"username\":\"test\",\"password\":\"123456\"}"))
                .andExpect(status().isOk());
    }
}
