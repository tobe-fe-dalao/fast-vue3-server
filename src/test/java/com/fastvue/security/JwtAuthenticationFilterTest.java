package com.fastvue.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.context.SecurityContextHolder;
import com.fastvue.infrastructure.tenant.TenantContext;
import com.fastvue.module.tenant.persistence.TenantEntity;
import com.fastvue.module.tenant.persistence.TenantMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Regression tests for JWT token-purpose separation. */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private TenantMapper tenantMapper;

    @Mock
    private FilterChain filterChain;

    @Mock
    private Claims claims;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    @DisplayName("Refresh Token 不能建立请求认证")
    void refreshTokenCannotAuthenticateRequest() throws Exception {
        MockHttpServletRequest request = bearerRequest("refresh-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtTokenProvider.parse("refresh-token")).thenReturn(claims);
        when(claims.get("type")).thenReturn("refresh");

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(userDetailsService);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Access Token 建立请求认证并保留权限")
    void accessTokenAuthenticatesRequest() throws Exception {
        MockHttpServletRequest request = bearerRequest("access-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        UserDetails user = User.withUsername("admin")
                .password("ignored")
                .authorities("analytics:view")
                .build();
        when(jwtTokenProvider.parse("access-token")).thenReturn(claims);
        when(claims.get("type")).thenReturn("access");
        when(claims.get("tenantId", Long.class)).thenReturn(1L);
        when(claims.get("username", String.class)).thenReturn("admin");
        TenantEntity tenant = new TenantEntity();
        tenant.setId(1L);
        tenant.setStatus("active");
        when(tenantMapper.selectById(1L)).thenReturn(tenant);
        when(userDetailsService.loadUserByUsername("admin")).thenReturn(user);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .isNotNull()
                .satisfies(authentication -> {
                    assertThat(authentication.getName()).isEqualTo("admin");
                    assertThat(authentication.getAuthorities())
                            .extracting("authority")
                            .containsExactly("analytics:view");
                });
        verify(filterChain).doFilter(request, response);
        verify(jwtTokenProvider, never()).parse("refresh-token");
    }

    @Test
    @DisplayName("停用租户的 Access Token 不能建立请求认证")
    void disabledTenantCannotAuthenticateRequest() throws Exception {
        MockHttpServletRequest request = bearerRequest("access-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        TenantEntity tenant = new TenantEntity();
        tenant.setId(9L);
        tenant.setStatus("disabled");
        when(jwtTokenProvider.parse("access-token")).thenReturn(claims);
        when(claims.get("type")).thenReturn("access");
        when(claims.get("tenantId", Long.class)).thenReturn(9L);
        when(tenantMapper.selectById(9L)).thenReturn(tenant);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(userDetailsService);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("租户用户即使持有同名 admin 角色也不获得跨租户权限")
    void tenantAdminRoleCannotBecomeSystemSuperAdministrator() throws Exception {
        MockHttpServletRequest request = bearerRequest("tenant-access-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        TenantEntity tenant = new TenantEntity();
        tenant.setId(9L);
        tenant.setStatus("active");
        LoginUser user = new LoginUser(7L, 9L, "tenant-user", "ignored", true,
                java.util.List.of("admin"), java.util.List.of());
        when(jwtTokenProvider.parse("tenant-access-token")).thenReturn(claims);
        when(claims.get("type")).thenReturn("access");
        when(claims.get("tenantId", Long.class)).thenReturn(9L);
        when(claims.get("username", String.class)).thenReturn("tenant-user");
        when(claims.getSubject()).thenReturn("7");
        when(tenantMapper.selectById(9L)).thenReturn(tenant);
        when(userDetailsService.loadUserByUsername("tenant-user")).thenReturn(user);
        doAnswer(invocation -> {
            assertThat(TenantContext.tenantId()).isEqualTo(9L);
            assertThat(TenantContext.isSuperAdmin()).isFalse();
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(TenantContext.tenantId()).isNull();
    }

    private MockHttpServletRequest bearerRequest(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }
}
