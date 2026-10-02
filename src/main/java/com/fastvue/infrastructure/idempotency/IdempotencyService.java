package com.fastvue.infrastructure.idempotency;

import com.fastvue.common.exception.BusinessException;
import com.fastvue.common.exception.ErrorCode;
import com.fastvue.infrastructure.tenant.TenantContext;
import com.fastvue.security.SecurityUtils;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The claim and business command commit together; rollback releases the claim. */
@Service
@RequiredArgsConstructor
public class IdempotencyService {
    private final JdbcTemplate jdbc;

    @Transactional
    public <T> T execute(String operation, String key, Supplier<T> action) {
        if (key == null || !key.matches("[\\x21-\\x7E]{1,128}")) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR, "Idempotency-Key 必须是 1-128 位可见 ASCII 字符");
        }
        Long tenantId = TenantContext.tenantId();
        Long userId = SecurityUtils.currentUserIdOrNull();
        if (tenantId == null || userId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        int claimed =
                jdbc.update(
                        """
                INSERT INTO sys_idempotency (tenant_id, user_id, operation, request_key)
                VALUES (?, ?, ?, ?) ON CONFLICT DO NOTHING
                """,
                        tenantId,
                        userId,
                        operation,
                        key);
        if (claimed == 0) throw new BusinessException(ErrorCode.IDEMPOTENCY_CONFLICT);
        return action.get();
    }
}
