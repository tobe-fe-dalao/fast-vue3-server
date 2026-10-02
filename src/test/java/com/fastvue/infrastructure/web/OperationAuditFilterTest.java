package com.fastvue.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.fastvue.module.audit.persistence.OperationLogEntity;
import com.fastvue.module.audit.service.OperationLogService;
import jakarta.servlet.ServletException;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
class OperationAuditFilterTest {
    @Mock OperationLogService service;

    @AfterEach
    void clear() {
        MDC.clear();
    }

    @Test
    void handledResponseKeepsStatusAndRestoresCallingContext() throws Exception {
        Map<String, String> previous =
                Map.of("traceId", "trace-1", "tenantId", "outer-tenant", "userId", "outer-user");
        MDC.setContextMap(previous);
        var request = new MockHttpServletRequest("GET", "/api/v1/projects");
        request.setAttribute("requestTenantId", 1L);
        request.setAttribute("requestUserId", 7L);
        var response = new MockHttpServletResponse();
        response.setHeader("X-Request-Id", "request-123");

        new OperationAuditFilter(service)
                .doFilter(
                        request,
                        response,
                        (req, res) -> {
                            assertThat(MDC.get("tenantId")).isEqualTo("1");
                            assertThat(MDC.get("userId")).isEqualTo("7");
                            response.setStatus(403);
                        });

        var log = ArgumentCaptor.forClass(OperationLogEntity.class);
        verify(service).save(log.capture());
        assertThat(log.getValue().getStatus()).isEqualTo(403);
        assertThat(log.getValue().getTenantId()).isEqualTo(1L);
        assertThat(log.getValue().getUserId()).isEqualTo(7L);
        assertThat(log.getValue().getRequestId()).isEqualTo("request-123");
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(MDC.getCopyOfContextMap()).isEqualTo(previous);
    }

    @Test
    void unhandledFailureIsNotAuditedAsSuccessfulResponse() {
        var failure = new ServletException("Request failed");
        var response = new MockHttpServletResponse();

        assertThatThrownBy(
                        () ->
                                new OperationAuditFilter(service)
                                        .doFilter(
                                                new MockHttpServletRequest(
                                                        "GET", "/api/v1/projects"),
                                                response,
                                                (req, res) -> {
                                                    throw failure;
                                                }))
                .isSameAs(failure);

        var log = ArgumentCaptor.forClass(OperationLogEntity.class);
        verify(service).save(log.capture());
        assertThat(log.getValue().getStatus()).isEqualTo(500);
        assertThat(response.getStatus())
                .isEqualTo(200); // The servlet container still owns exception handling.
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    @Test
    void auditFailureDoesNotChangeResponseOrLoseCallingContext() throws Exception {
        Map<String, String> previous = Map.of("traceId", "trace-2", "tenantId", "outer-tenant");
        MDC.setContextMap(previous);
        doThrow(new IllegalStateException("Database unavailable")).when(service).save(any());
        var response = new MockHttpServletResponse();

        new OperationAuditFilter(service)
                .doFilter(
                        new MockHttpServletRequest(),
                        response,
                        (req, res) -> {
                            response.setStatus(201);
                            response.getWriter().write("unchanged");
                        });

        assertThat(response.getStatus()).isEqualTo(201);
        assertThat(response.getContentAsString()).isEqualTo("unchanged");
        assertThat(MDC.getCopyOfContextMap()).isEqualTo(previous);
    }
}
