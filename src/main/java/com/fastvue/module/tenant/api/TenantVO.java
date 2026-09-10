package com.fastvue.module.tenant.api;

import java.time.OffsetDateTime;

public record TenantVO(Long id, String name, String code, String status, String plan,
                       OffsetDateTime expiredAt, OffsetDateTime createdAt) {
}
