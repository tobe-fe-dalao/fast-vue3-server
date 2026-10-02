package com.fastvue.module.portal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fastvue.module.portal.api.PortalController;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** 门户公开 API 的 MVC 契约测试。 */
@WebMvcTest(PortalController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class PortalControllerWebTest extends com.fastvue.security.SecurityMvcTestSupport {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private JwtTokenProvider jwtTokenProvider;

    @MockitoBean private TenantMapper tenantMapper;

    @MockitoBean private RefreshTokenStore refreshTokenStore;

    @MockitoBean private UserDetailsService userDetailsService;

    @Test
    @DisplayName("未登录也可分页读取博客及分类")
    void blogListIsPublic() throws Exception {
        mockMvc.perform(
                        get("/api/v1/public/blog")
                                .queryParam("page", "1")
                                .queryParam("pageSize", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(2))
                .andExpect(jsonPath("$.data.categories[0]").value("全部"));
    }

    @Test
    @DisplayName("博客详情不存在时返回统一业务错误")
    void missingBlogUsesApiEnvelope() throws Exception {
        mockMvc.perform(get("/api/v1/public/blog/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").isNumber())
                .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.not(0)))
                .andExpect(jsonPath("$.message").value("文章不存在"));
    }

    @Test
    @DisplayName("未登录可提交联系表单")
    void contactIsPublic() throws Exception {
        mockMvc.perform(
                        post("/api/v1/public/contact")
                                .contentType("application/json")
                                .content(
                                        "{\"email\":\"dev@example.com\",\"name\":\"开发者\",\"message\":\"hello\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.accepted").value(true))
                .andExpect(jsonPath("$.data.email").value("dev@example.com"));
    }

    @Test
    @DisplayName("未登录可读取价格、FAQ、首页、产品、功能、关于和文档")
    void allContentReadsArePublic() throws Exception {
        for (String path :
                new String[] {"pricing", "faq", "home", "product", "features", "about", "docs"}) {
            mockMvc.perform(get("/api/v1/public/" + path))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
        }
    }
}
