package com.fastvue.config;

import com.fastvue.infrastructure.web.OperationAuditFilter;
import com.fastvue.module.audit.service.OperationLogService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OperationAuditConfig {
    @Bean
    OperationAuditFilter operationAuditFilter(OperationLogService service) {
        return new OperationAuditFilter(service);
    }
}
