package com.fastvue.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.servlet.ServletException;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestCorrelationFilterTest {
    private final RequestCorrelationFilter filter = new RequestCorrelationFilter();

    @AfterEach
    void clear() {
        MDC.clear();
    }

    @Test
    void successfulRequestRestoresCallingContextAndKeepsResponseId() throws Exception {
        Map<String, String> previous = Map.of("traceId", "trace-1", "requestId", "outer-request");
        MDC.setContextMap(previous);
        var request = new MockHttpServletRequest("GET", "/api/v1/projects");
        request.addHeader("X-Request-Id", "request-123");
        request.setAttribute("requestTenantId", 1L);
        request.setAttribute("requestUserId", 7L);
        var response = new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                (req, res) -> {
                    assertThat(MDC.get("requestId")).isEqualTo("request-123");
                    assertThat(MDC.get("traceId")).isEqualTo("trace-1");
                    response.setStatus(202);
                });

        assertThat(response.getHeader("X-Request-Id")).isEqualTo("request-123");
        assertThat(response.getStatus()).isEqualTo(202);
        assertThat(MDC.getCopyOfContextMap()).isEqualTo(previous);
    }

    @Test
    void failedRequestRestoresContextAndPropagatesOriginalException() {
        Map<String, String> previous = Map.of("traceId", "trace-2", "tenantId", "outer-tenant");
        MDC.setContextMap(previous);
        var failure = new ServletException("Request failed");

        assertThatThrownBy(
                        () ->
                                filter.doFilter(
                                        new MockHttpServletRequest(),
                                        new MockHttpServletResponse(),
                                        (req, res) -> {
                                            throw failure;
                                        }))
                .isSameAs(failure);

        assertThat(MDC.getCopyOfContextMap()).isEqualTo(previous);
    }

    @Test
    void requestWithoutCallingContextLeavesNoMdcValues() throws Exception {
        MDC.clear();
        var request = new MockHttpServletRequest();
        request.setAttribute("requestTenantId", 1L);
        request.setAttribute("requestUserId", 7L);
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {});

        assertThat(response.getHeader("X-Request-Id")).isNotBlank();
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    @Test
    void invalidRequestIdsNeverReachResponseHeaders() throws Exception {
        for (String supplied :
                java.util.List.of(
                        "bad\r\nInjected: yes", "with space", "含中文", "", "a".repeat(65))) {
            var request = new MockHttpServletRequest();
            request.addHeader("X-Request-Id", supplied);
            var response = new MockHttpServletResponse();
            filter.doFilter(request, response, (req, res) -> {});
            String generated = response.getHeader("X-Request-Id");
            assertThat(generated).isNotEqualTo(supplied).matches("[A-Za-z0-9._-]{1,64}");
            assertThat(java.util.UUID.fromString(generated).toString()).isEqualTo(generated);
            assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
        }
    }
}
