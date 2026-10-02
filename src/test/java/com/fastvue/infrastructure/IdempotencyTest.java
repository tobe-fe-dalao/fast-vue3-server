package com.fastvue.infrastructure;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fastvue.common.exception.BusinessException;
import com.fastvue.infrastructure.idempotency.IdempotencyService;
import com.fastvue.infrastructure.tenant.TenantContext;
import com.fastvue.security.LoginUser;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class IdempotencyTest {
    @AfterEach
    void clear() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void duplicateDoesNotRunCommand() {
        TenantContext.set(1L, false);
        var user = new LoginUser(7L, 1L, "user", "x", true, List.of(), List.of());
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
        var jdbc = mock(JdbcTemplate.class);
        var service = new IdempotencyService(jdbc);
        assertThatThrownBy(
                        () ->
                                service.execute(
                                        "create",
                                        "key",
                                        () -> {
                                            throw new AssertionError("must not execute");
                                        }))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("幂等");
    }

    @Test
    void malformedKeyRejectedBeforeDatabaseAccess() {
        var jdbc = mock(JdbcTemplate.class);
        assertThatThrownBy(
                        () ->
                                new IdempotencyService(jdbc)
                                        .execute("create", "bad key", () -> "unused"))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(jdbc);
    }
}
