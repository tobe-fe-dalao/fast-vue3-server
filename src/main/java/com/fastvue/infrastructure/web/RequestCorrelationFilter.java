package com.fastvue.infrastructure.web;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class RequestCorrelationFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Map<String, String> previousMdc = MDC.getCopyOfContextMap();
        String id = request.getHeader("X-Request-Id");
        if (id == null || !id.matches("[A-Za-z0-9._-]{1,64}")) id = UUID.randomUUID().toString();
        MDC.put("requestId", id);
        response.setHeader("X-Request-Id", id);
        long start = System.nanoTime();
        boolean failed = true;
        try {
            chain.doFilter(request, response);
            failed = false;
        } finally {
            try {
                // No query strings, request bodies, headers or credentials in access logs.
                Object tenant = request.getAttribute("requestTenantId");
                Object user = request.getAttribute("requestUserId");
                if (tenant != null) MDC.put("tenantId", tenant.toString());
                if (user != null) MDC.put("userId", user.toString());
                Object route =
                        request.getAttribute(
                                org.springframework.web.servlet.HandlerMapping
                                        .BEST_MATCHING_PATTERN_ATTRIBUTE);
                log.info(
                        "http method={} route={} status={} durationMs={}",
                        request.getMethod(),
                        route == null ? "<unmapped>" : route,
                        failed ? 500 : response.getStatus(),
                        (System.nanoTime() - start) / 1_000_000);
            } finally {
                if (previousMdc == null) MDC.clear();
                else MDC.setContextMap(previousMdc);
            }
        }
    }
}
