package com.fastvue.module.audit.api;

import java.time.OffsetDateTime;

public record OperationLogVO(Long id, String requestId, Long tenantId, Long userId, String method,
        String path, String ip, String userAgent, Long duration, Integer status, OffsetDateTime createdAt) {}
