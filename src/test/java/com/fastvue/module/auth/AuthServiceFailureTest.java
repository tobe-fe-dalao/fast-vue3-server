package com.fastvue.module.auth;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fastvue.common.exception.*;
import com.fastvue.infrastructure.tenant.TenantContext;
import com.fastvue.module.auth.api.LoginRequest;
import com.fastvue.module.auth.service.AuthService;
import com.fastvue.module.tenant.persistence.TenantEntity;
import com.fastvue.module.tenant.service.TenantService;
import com.fastvue.module.user.persistence.UserMapper;
import com.fastvue.security.*;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.*;

class AuthServiceFailureTest {
    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    @Test
    void disabledAccountStillReturns401() {
        assertFailure(new DisabledException("disabled"), ErrorCode.INVALID_USERNAME_OR_PASSWORD);
    }

    @Test
    void missingAuthenticationProviderIsAServerError() {
        assertFailure(
                new ProviderNotFoundException("provider configuration"), ErrorCode.INTERNAL_ERROR);
    }

    @Test
    void credentialFailureStillReturns401() {
        assertFailure(
                new BadCredentialsException("invalid"), ErrorCode.INVALID_USERNAME_OR_PASSWORD);
    }

    @Test
    void databaseAuthenticationFailureReturns503WithoutIssuingTokens() {
        assertFailure(
                new InternalAuthenticationServiceException(
                        "private JDBC details",
                        new org.springframework.dao.DataAccessResourceFailureException("down")),
                ErrorCode.SERVICE_UNAVAILABLE);
    }

    @Test
    void authenticationInfrastructureFailureReturns503() {
        assertFailure(new AuthenticationServiceException("down"), ErrorCode.SERVICE_UNAVAILABLE);
    }

    private void assertFailure(
            org.springframework.security.core.AuthenticationException failure, ErrorCode expected) {
        var manager = mock(AuthenticationManager.class);
        var tokens = mock(JwtTokenProvider.class);
        var refresh = mock(RefreshTokenStore.class);
        var users = mock(UserMapper.class);
        var tenants = mock(TenantService.class);
        var tenant = new TenantEntity();
        tenant.setId(1L);
        when(tenants.requireActive("default")).thenReturn(tenant);
        when(manager.authenticate(any())).thenThrow(failure);
        var service = new AuthService(manager, tokens, refresh, users, tenants);
        TenantContext.set(7L, false);
        assertThatThrownBy(() -> service.login(new LoginRequest("alice", "password", "default")))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(expected);
        assertThat(TenantContext.tenantId()).isEqualTo(7L);
        verifyNoInteractions(tokens, refresh, users);
    }
}
