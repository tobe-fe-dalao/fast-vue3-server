package com.fastvue.security;

import com.fastvue.infrastructure.tenant.TenantContext;
import com.fastvue.module.tenant.persistence.TenantEntity;
import com.fastvue.module.tenant.persistence.TenantMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

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
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        TenantContext.clear();
        try {
            authenticate(request);
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private void authenticate(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) return;
        try {
            Claims claims = jwtTokenProvider.parse(header.substring(7));
            if (!"access".equals(claims.get("type"))) return;
            Long tenantId = claims.get("tenantId", Long.class);
            if (tenantId == null) return;
            TenantEntity tenant = tenantMapper.selectById(tenantId);
            if (tenant == null
                    || !"active".equals(tenant.getStatus())
                    || tenant.getExpiredAt() != null
                            && tenant.getExpiredAt().isBefore(OffsetDateTime.now())) return;
            String username = claims.get("username", String.class);
            if (username == null || SecurityContextHolder.getContext().getAuthentication() != null)
                return;
            TenantContext.set(tenantId, false);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            if (!userDetails.isEnabled()
                    || userDetails instanceof LoginUser user
                            && (!tenantId.equals(user.tenantId())
                                    || !String.valueOf(user.id()).equals(claims.getSubject()))) {
                TenantContext.clear();
                return;
            }
            boolean superAdmin =
                    userDetails instanceof LoginUser user
                            && Long.valueOf(1L).equals(user.id())
                            && Long.valueOf(1L).equals(user.tenantId())
                            && user.roles().contains("admin");
            TenantContext.set(tenantId, superAdmin);
            var authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            request.setAttribute("requestTenantId", tenantId);
            if (userDetails instanceof LoginUser user)
                request.setAttribute("requestUserId", user.id());
        } catch (JwtException
                | IllegalArgumentException
                | org.springframework.security.core.AuthenticationException ex) {
            SecurityContextHolder.clearContext();
            TenantContext.clear();
        }
    }
}
