package com.fastvue.module.analytics;

import com.fastvue.common.GlobalExceptionHandler;
import com.fastvue.security.JwtAuthenticationFilter;
import com.fastvue.security.JwtTokenProvider;
import com.fastvue.security.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Permission tests for operating-data endpoints. */
@WebMvcTest({AnalyticsController.class, DataController.class})
@Import({SecurityConfig.class, GlobalExceptionHandler.class, JwtAuthenticationFilter.class})
class AnalyticsControllerPermissionTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("匿名访问经营数据返回统一 401")
    void anonymousRequestIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/overview"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @WithMockUser(authorities = "dashboard:view")
    @DisplayName("已登录但缺少权限返回统一 403")
    void missingPermissionIsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/data/overview"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @WithMockUser(authorities = "analytics:view")
    @DisplayName("analytics:view 可以读取分析数据")
    void analyticsPermissionAllowsRead() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @WithMockUser(authorities = "data:view")
    @DisplayName("data:view 可以读取数据中心")
    void dataPermissionAllowsRead() throws Exception {
        mockMvc.perform(get("/api/v1/data/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }
}
