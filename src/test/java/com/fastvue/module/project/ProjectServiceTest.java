package com.fastvue.module.project;

import com.fastvue.infrastructure.metrics.BusinessMetrics;
import com.fastvue.common.exception.BusinessException;
import com.fastvue.common.exception.ErrorCode;
import com.fastvue.module.project.api.ProjectModels.CreateRequest;
import com.fastvue.module.project.persistence.ProjectEntity;
import com.fastvue.module.project.persistence.ProjectMapper;
import com.fastvue.module.project.service.ProjectService;
import com.fastvue.module.user.persistence.UserMapper;
import com.fastvue.security.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {
    @Mock ProjectMapper mapper;
    @Mock UserMapper userMapper;
    @Mock BusinessMetrics metrics;
    @Mock com.fastvue.module.project.persistence.ProjectActivityMapper activityMapper;
    @Mock com.fastvue.module.task.persistence.TaskMapper taskMapper;
    ProjectService service;

    @BeforeEach void setUp() {
        service = new ProjectService(mapper, userMapper, metrics, activityMapper, taskMapper);
        LoginUser user = new LoginUser(7L, 1L, "owner", "x", true, List.of("tenant-admin"), List.of());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void creatorBecomesProjectOwnerAndMember() {
        doAnswer(invocation -> { ((ProjectEntity) invocation.getArgument(0)).setId(42L); return 1; })
                .when(mapper).insert(any(ProjectEntity.class));
        when(mapper.selectMemberIds(42L)).thenReturn(List.of(7L));
        var result = service.create(new CreateRequest("Enterprise", "ENT", null, null, null));
        assertThat(result.ownerId()).isEqualTo(7L);
        verify(mapper).addMember(1L, 42L, 7L, "OWNER");
        verify(metrics).projectCreated();
    }

    @Test void archivedProjectRejectsMemberRemoval() {
        ProjectEntity project = new ProjectEntity();
        project.setId(42L); project.setTenantId(1L); project.setOwnerId(7L);
        project.setStatus("ARCHIVED");
        when(mapper.selectById(42L)).thenReturn(project);
        when(mapper.isMember(42L, 7L)).thenReturn(true);

        assertThatThrownBy(() -> service.removeMember(42L, 8L))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT);
        verify(mapper, never()).removeMember(any(), any());
    }
}
