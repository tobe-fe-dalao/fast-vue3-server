package com.fastvue.security;

import com.fastvue.common.exception.BusinessException;
import com.fastvue.common.exception.ErrorCode;
import com.fastvue.infrastructure.metrics.BusinessMetrics;
import com.fastvue.module.project.persistence.ProjectEntity;
import com.fastvue.module.project.persistence.ProjectMapper;
import com.fastvue.module.project.service.ProjectService;
import com.fastvue.module.user.persistence.UserMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityAccessTest {
    @Mock ProjectMapper mapper; @Mock UserMapper users; @Mock BusinessMetrics metrics;
    @Mock com.fastvue.module.project.persistence.ProjectActivityMapper activityMapper;
    @Mock com.fastvue.module.task.persistence.TaskMapper taskMapper;
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void nonMemberCannotUseProjectIdAsIdor() {
        ProjectEntity project = new ProjectEntity(); project.setId(99L); project.setTenantId(1L);
        when(mapper.selectById(99L)).thenReturn(project);
        when(mapper.isMember(99L, 7L)).thenReturn(false);
        LoginUser user = new LoginUser(7L, 1L, "outsider", "x", true, List.of(), List.of());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));

        ProjectService service = new ProjectService(mapper, users, metrics, activityMapper, taskMapper);
        assertThatThrownBy(() -> service.get(99L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PROJECT_NOT_FOUND);
    }

    @Test void tenantAdministratorStillNeedsProjectMembership() {
        ProjectEntity project = new ProjectEntity(); project.setId(100L); project.setTenantId(1L);
        when(mapper.selectById(100L)).thenReturn(project);
        LoginUser user = new LoginUser(7L, 1L, "tenant-admin", "x", true,
                List.of("tenant-admin"), List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));

        ProjectService service = new ProjectService(mapper, users, metrics, activityMapper, taskMapper);
        assertThatThrownBy(() -> service.get(100L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PROJECT_NOT_FOUND);
    }
}
