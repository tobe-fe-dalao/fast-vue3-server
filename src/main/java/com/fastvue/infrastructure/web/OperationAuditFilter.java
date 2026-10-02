package com.fastvue.infrastructure.web;

import com.fastvue.module.audit.persistence.OperationLogEntity;
import com.fastvue.module.audit.service.OperationLogService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Order(Ordered.LOWEST_PRECEDENCE - 20)
@RequiredArgsConstructor
public class OperationAuditFilter extends OncePerRequestFilter {
    private final OperationLogService service;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator")
                || path.startsWith("/api-docs-ui")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Map<String, String> previousMdc = MDC.getCopyOfContextMap();
        String requestId = response.getHeader("X-Request-Id");
        Object tenantId = request.getAttribute("requestTenantId");
        Object userId = request.getAttribute("requestUserId");
        if (tenantId != null) MDC.put("tenantId", String.valueOf(tenantId));
        if (userId != null) MDC.put("userId", String.valueOf(userId));
        long start = System.nanoTime();
        boolean failed = true;
        try {
            chain.doFilter(request, response);
            failed = false;
        } finally {
            try {
                OperationLogEntity value = new OperationLogEntity();
                value.setRequestId(requestId);
                value.setTenantId(asLong(request.getAttribute("requestTenantId")));
                value.setUserId(asLong(request.getAttribute("requestUserId")));
                value.setMethod(request.getMethod());
                value.setPath(truncate(request.getRequestURI(), 512));
                value.setIp(truncate(clientIp(request), 64));
                value.setUserAgent(truncate(request.getHeader("User-Agent"), 512));
                value.setDuration((System.nanoTime() - start) / 1_000_000L);
                value.setStatus(failed ? 500 : response.getStatus());
                value.setCreatedAt(OffsetDateTime.now());
                service.save(value);
            } catch (Exception ex) {
                log.warn("操作审计写入失败 requestId={} ({})", requestId, ex.getClass().getSimpleName());
            } finally {
                if (previousMdc == null) MDC.clear();
                else MDC.setContextMap(previousMdc);
            }
        }
    }

    private String clientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    private String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }

    private Long asLong(Object value) {
        return value instanceof Long number ? number : null;
    }
}
