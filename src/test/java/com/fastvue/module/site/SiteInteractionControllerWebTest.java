package com.fastvue.module.site;

import com.fastvue.module.site.api.BlogCommentVO;
import com.fastvue.module.site.api.PaymentOrderVO;
import com.fastvue.module.site.api.SiteInteractionController;
import com.fastvue.module.site.service.SiteInteractionService;
import com.fastvue.module.tenant.persistence.TenantMapper;
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

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Authentication boundary and response-contract tests for site interactions. */
@WebMvcTest(SiteInteractionController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class SiteInteractionControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SiteInteractionService service;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private TenantMapper tenantMapper;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("评论列表允许匿名读取")
    void commentsArePublic() throws Exception {
        when(service.comments(1L)).thenReturn(List.of(
                new BlogCommentVO(1L, 1L, "admin", "有帮助", OffsetDateTime.now())));

        mockMvc.perform(get("/api/v1/public/blog/1/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].content").value("有帮助"));
    }

    @Test
    @DisplayName("匿名提交评论和创建支付订单均返回 401")
    void protectedWritesRejectAnonymousUsers() throws Exception {
        mockMvc.perform(post("/api/v1/blog/1/comments")
                        .contentType("application/json")
                        .content("{\"content\":\"anonymous\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));

        mockMvc.perform(post("/api/v1/payments/checkout")
                        .contentType("application/json")
                        .content("{\"planId\":2,\"channel\":\"alipay\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("登录用户可以提交评论并创建待支付订单")
    void authenticatedUserCanWrite() throws Exception {
        OffsetDateTime now = OffsetDateTime.now();
        when(service.createComment(any(), any()))
                .thenReturn(new BlogCommentVO(2L, 1L, "admin", "登录评论", now));
        when(service.checkout(any()))
                .thenReturn(new PaymentOrderVO(1L, "FV1001", 2L, "团队版", 29900,
                        "CNY", "alipay", "pending", "/payment/demo/FV1001", now, now.plusMinutes(15)));

        mockMvc.perform(post("/api/v1/blog/1/comments")
                        .contentType("application/json")
                        .content("{\"content\":\"登录评论\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("admin"));

        mockMvc.perform(post("/api/v1/payments/checkout")
                        .contentType("application/json")
                        .content("{\"planId\":2,\"channel\":\"alipay\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.amountCents").value(29900))
                .andExpect(jsonPath("$.data.status").value("pending"));
    }
}
