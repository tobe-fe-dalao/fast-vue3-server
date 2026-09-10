package com.fastvue.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fastvue.infrastructure.tenant.PublicTenantFilter;
import com.fastvue.module.tenant.persistence.TenantMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registers anonymous tenant resolution in the full application context. */
@Configuration
public class PublicTenantFilterConfig {
    @Bean
    public PublicTenantFilter publicTenantFilter(TenantMapper tenantMapper, ObjectMapper objectMapper) {
        return new PublicTenantFilter(tenantMapper, objectMapper);
    }
}
