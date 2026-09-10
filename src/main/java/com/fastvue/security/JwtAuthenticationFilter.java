package com.fastvue.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import com.fastvue.infrastructure.tenant.TenantContext;
import com.fastvue.module.tenant.persistence.TenantEntity;
import com.fastvue.module.tenant.persistence.TenantMapper;

import java.time.OffsetDateTime;

/**
 * JWT 认证过滤器。
 *
 * <p>从 {@code Authorization: Bearer <token>} 中解析 Access Token，校验通过后将
 * 认证信息写入 {@link SecurityContextHolder}，供后续授权判断使用。</p>
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    private final TenantMapper tenantMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        TenantContext.clear();
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        try {
            Claims claims = jwtTokenProvider.parse(token);
            // 仅 Access Token 可用于认证，Refresh Token 不能当作会话凭证
            if (!"access".equals(claims.get("type"))) {
                filterChain.doFilter(request, response);
                return;
            }

            Long tenantId = claims.get("tenantId", Long.class);
            if (tenantId == null) {
                tenantId = 1L;
            }
            TenantEntity tenant = tenantMapper.selectById(tenantId);
            if (tenant == null || !"active".equals(tenant.getStatus())
                    || tenant.getExpiredAt() != null && tenant.getExpiredAt().isBefore(OffsetDateTime.now())) {
                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }
            String username = claims.get("username", String.class);
            TenantContext.set(tenantId, false);
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                if (!userDetails.isEnabled() || userDetails instanceof LoginUser loginUser
                        && (!tenantId.equals(loginUser.tenantId())
                        || !String.valueOf(loginUser.id()).equals(claims.getSubject()))) {
                    SecurityContextHolder.clearContext();
                    TenantContext.clear();
                    filterChain.doFilter(request, response);
                    return;
                }
                boolean superAdmin = userDetails instanceof LoginUser loginUser
                        && loginUser.id().equals(1L)
                        && loginUser.tenantId().equals(1L)
                        && loginUser.roles().contains("admin");
                TenantContext.set(tenantId, superAdmin);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                request.setAttribute("requestTenantId", tenantId);
                if (userDetails instanceof LoginUser loginUser) {
                    request.setAttribute("requestUserId", loginUser.id());
                }
            }
        } catch (JwtException | IllegalArgumentException | org.springframework.security.core.AuthenticationException ex) {
            // Token 无效或过期：不设置认证信息，由后续 EntryPoint 返回 401
            SecurityContextHolder.clearContext();
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
