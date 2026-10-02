package com.fastvue.module.demo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fastvue.module.demo.api.DemoDataController;
import com.fastvue.module.tenant.persistence.TenantMapper;
import com.fastvue.security.JwtAuthenticationFilter;
import com.fastvue.security.JwtTokenProvider;
import com.fastvue.security.RefreshTokenStore;
import com.fastvue.security.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** 管理端聚合数据接口的安全与分页契约测试。 */
@WebMvcTest(DemoDataController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class DemoDataControllerWebTest extends com.fastvue.security.SecurityMvcTestSupport {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private JwtTokenProvider jwtTokenProvider;

    @MockitoBean private TenantMapper tenantMapper;

    @MockitoBean private RefreshTokenStore refreshTokenStore;

    @MockitoBean private UserDetailsService userDetailsService;

    @Test
    @DisplayName("管理端演示接口未登录返回 401")
    void dashboardRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/stats"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").isNumber());
    }

    @Test
    @DisplayName("已登录可读取分页登录日志")
    @WithMockUser
    void loginLogHasCanonicalPageShape() throws Exception {
        mockMvc.perform(
                        get("/api/v1/log/login")
                                .queryParam("page", "2")
                                .queryParam("pageSize", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.items.length()").value(5))
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.pageSize").value(5))
                .andExpect(jsonPath("$.data.total").value(50));
    }

    @Test
    @DisplayName("在线用户接口返回 items 与 total")
    @WithMockUser
    void onlineUsersHasCanonicalListShape() throws Exception {
        mockMvc.perform(get("/api/v1/monitor/online"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.total").value(2));
    }
}
