package com.fastvue.infrastructure.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TenantContextTest {
    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    @Test
    void nestedScopesRestoreTenantAndAdministratorState() {
        TenantContext.set(1L, true);
        String result =
                TenantContext.runAs(
                        2L,
                        false,
                        () -> {
                            assertThat(TenantContext.tenantId()).isEqualTo(2L);
                            assertThat(TenantContext.isSuperAdmin()).isFalse();
                            TenantContext.runAs(
                                    3L,
                                    true,
                                    () -> {
                                        assertThat(TenantContext.tenantId()).isEqualTo(3L);
                                        assertThat(TenantContext.isSuperAdmin()).isTrue();
                                    });
                            assertThat(TenantContext.tenantId()).isEqualTo(2L);
                            assertThat(TenantContext.isSuperAdmin()).isFalse();
                            return "result";
                        });
        assertThat(result).isEqualTo("result");
        assertThat(TenantContext.tenantId()).isEqualTo(1L);
        assertThat(TenantContext.isSuperAdmin()).isTrue();
    }

    @Test
    void exceptionRestoresPreviouslyAuthenticatedTenant() {
        TenantContext.set(1L, false);
        assertThatThrownBy(
                        () ->
                                TenantContext.runAs(
                                        2L,
                                        true,
                                        () -> {
                                            throw new IllegalStateException("failed operation");
                                        }))
                .isInstanceOf(IllegalStateException.class);
        assertThat(TenantContext.tenantId()).isEqualTo(1L);
        assertThat(TenantContext.isSuperAdmin()).isFalse();
    }

    @Test
    void exceptionDoesNotLeakIdentityIntoPreviouslyAnonymousThread() {
        assertThatThrownBy(
                        () ->
                                TenantContext.runAs(
                                        2L,
                                        true,
                                        () -> {
                                            throw new IllegalStateException("failed operation");
                                        }))
                .isInstanceOf(IllegalStateException.class);
        assertThat(TenantContext.tenantId()).isNull();
        assertThat(TenantContext.isSuperAdmin()).isFalse();
    }

    @Test
    void tenantIdentityIsNotInheritedByOtherThreads() throws Exception {
        TenantContext.set(1L, true);
        try (var executor = Executors.newSingleThreadExecutor()) {
            assertThat(executor.submit(TenantContext::tenantId).get()).isNull();
            assertThat(executor.submit(TenantContext::isSuperAdmin).get()).isFalse();
        }
        assertThat(TenantContext.tenantId()).isEqualTo(1L);
    }
}
