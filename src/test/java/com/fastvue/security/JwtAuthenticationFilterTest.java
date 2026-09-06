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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
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
    private FilterChain filterChain;

    @Mock
    private Claims claims;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
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
        when(claims.get("username", String.class)).thenReturn("admin");
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

    private MockHttpServletRequest bearerRequest(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        return request;
    }
}
