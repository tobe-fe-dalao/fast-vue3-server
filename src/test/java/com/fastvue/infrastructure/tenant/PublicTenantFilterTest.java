package com.fastvue.infrastructure.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fastvue.module.tenant.persistence.TenantEntity;
import com.fastvue.module.tenant.persistence.TenantMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.OffsetDateTime;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PublicTenantFilterTest {

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    @Test
    void resolvesHeaderTenantForAnonymousPublicRequestAndClearsContextAfterwards() throws Exception {
        TenantMapper mapper = mock(TenantMapper.class);
        TenantEntity tenant = new TenantEntity();
        tenant.setId(23L); tenant.setCode("acme"); tenant.setStatus("active");
        when(mapper.selectOne(any())).thenReturn(tenant);
        PublicTenantFilter filter = new PublicTenantFilter(mapper, new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/public/blog/1/comments");
        request.addHeader(PublicTenantFilter.TENANT_CODE_HEADER, "acme");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicLong tenantSeenByController = new AtomicLong();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) ->
                tenantSeenByController.set(TenantContext.tenantId()));

        assertThat(tenantSeenByController.get()).isEqualTo(23L);
        assertThat(request.getAttribute("requestTenantId")).isEqualTo(23L);
        assertThat(TenantContext.tenantId()).isNull();
    }

    @Test
    void rejectsExpiredTenantBeforeController() throws Exception {
        TenantMapper mapper = mock(TenantMapper.class);
        TenantEntity tenant = new TenantEntity();
        tenant.setId(23L); tenant.setCode("acme"); tenant.setStatus("active");
        tenant.setExpiredAt(OffsetDateTime.now().minusMinutes(1));
        when(mapper.selectOne(any())).thenReturn(tenant);
        PublicTenantFilter filter = new PublicTenantFilter(mapper, new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/public/blog");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> {
            throw new AssertionError("expired tenant must not reach controller");
        });

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("租户已停用或已过期");
    }
}
