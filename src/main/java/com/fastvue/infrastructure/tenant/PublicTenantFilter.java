package com.fastvue.infrastructure.tenant;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fastvue.common.exception.ErrorCode;
import com.fastvue.common.response.ApiResponse;
import com.fastvue.module.tenant.persistence.TenantEntity;
import com.fastvue.module.tenant.persistence.TenantMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.OffsetDateTime;

/**
 * Resolves tenant identity for anonymous public APIs.
 *
 * <p>Authenticated requests already receive their tenant from the access token. Anonymous
 * requests select a tenant through {@code X-Tenant-Code}; omitting the header addresses the
 * backwards-compatible {@code default} tenant. This filter runs after Spring Security's filter
 * chain has attempted JWT authentication and before request audit persistence.</p>
 */
@Order(0)
@RequiredArgsConstructor
public class PublicTenantFilter extends OncePerRequestFilter {

    public static final String TENANT_CODE_HEADER = "X-Tenant-Code";

    private final TenantMapper tenantMapper;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/v1/public/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (TenantContext.tenantId() != null) {
            chain.doFilter(request, response);
            return;
        }

        String requestedCode = request.getHeader(TENANT_CODE_HEADER);
        String code = requestedCode == null || requestedCode.isBlank() ? "default" : requestedCode.trim();
        TenantEntity tenant = tenantMapper.selectOne(new LambdaQueryWrapper<TenantEntity>()
                .eq(TenantEntity::getCode, code));
        if (tenant == null) {
            writeError(response, ErrorCode.TENANT_NOT_FOUND);
            return;
        }
        if (!"active".equals(tenant.getStatus())
                || tenant.getExpiredAt() != null && tenant.getExpiredAt().isBefore(OffsetDateTime.now())) {
            writeError(response, ErrorCode.TENANT_DISABLED);
            return;
        }

        TenantContext.set(tenant.getId(), false);
        request.setAttribute("requestTenantId", tenant.getId());
        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private void writeError(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.code());
        response.setCharacterEncoding(java.nio.charset.StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(errorCode));
    }
}
