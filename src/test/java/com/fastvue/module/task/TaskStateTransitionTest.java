package com.fastvue.module.task;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fastvue.common.exception.BusinessException;
import com.fastvue.common.exception.ErrorCode;
import com.fastvue.infrastructure.metrics.BusinessMetrics;
import com.fastvue.module.project.persistence.ProjectEntity;
import com.fastvue.module.project.service.ProjectService;
import com.fastvue.module.task.api.TaskModels.UpdateRequest;
import com.fastvue.module.task.persistence.*;
import com.fastvue.module.task.service.TaskService;
import com.fastvue.security.LoginUser;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class TaskStateTransitionTest {
    @Mock TaskMapper taskMapper;
    @Mock TaskCommentMapper comments;
    @Mock TaskActivityMapper activities;
    @Mock ProjectService projects;
    @Mock ApplicationEventPublisher publisher;
    @Mock BusinessMetrics metrics;
    TaskService service;
    TaskEntity task;
    ProjectEntity project;

    @BeforeEach
    void setUp() {
        service = new TaskService(taskMapper, comments, activities, projects, publisher, metrics);
        task = new TaskEntity();
        task.setId(9L);
        task.setProjectId(2L);
        task.setStatus("IN_PROGRESS");
        task.setVersion(0);
        when(taskMapper.selectById(9L)).thenReturn(task);
        project = new ProjectEntity();
        project.setId(2L);
        project.setStatus("ACTIVE");
        when(projects.lockForMutation(2L)).thenReturn(project);
        LoginUser user = new LoginUser(1L, 1L, "user", "x", true, List.of(), List.of());
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void inProgressCanBecomeDone() {
        when(taskMapper.updateById(task)).thenReturn(1);
        service.update(9L, new UpdateRequest(null, null, null, null, "DONE", null, 0));
        assertThat(task.getStatus()).isEqualTo("DONE");
        verify(activities).insert(any(TaskActivityEntity.class));
    }

    @Test
    void doneTaskCannotBeEditedOrReopened() {
        task.setStatus("DONE");
        assertThatThrownBy(
                        () ->
                                service.update(
                                        9L,
                                        new UpdateRequest(
                                                "changed",
                                                null,
                                                null,
                                                null,
                                                "IN_PROGRESS",
                                                null,
                                                0)))
                .hasMessageContaining("不能编辑");
        verify(taskMapper, never()).updateById(any(TaskEntity.class));
    }

    @Test
    void archivedProjectRejectsTaskUpdatesWithoutAuditOrNotifications() {
        project.setStatus("ARCHIVED");
        assertThatThrownBy(
                        () ->
                                service.update(
                                        9L,
                                        new UpdateRequest(
                                                "changed", null, null, null, null, null, 0)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ILLEGAL_STATE_TRANSITION);
        verify(taskMapper, never()).updateById(any(TaskEntity.class));
        verifyNoInteractions(activities, publisher);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
        "TODO,IN_PROGRESS,true", "TODO,CANCELLED,true", "TODO,DONE,false", "TODO,BLOCKED,false",
        "IN_PROGRESS,BLOCKED,true", "IN_PROGRESS,DONE,true", "IN_PROGRESS,CANCELLED,true",
                "IN_PROGRESS,TODO,false",
        "BLOCKED,IN_PROGRESS,true", "BLOCKED,CANCELLED,true", "BLOCKED,DONE,false",
                "BLOCKED,TODO,false",
        "DONE,IN_PROGRESS,false", "DONE,DONE,false", "CANCELLED,TODO,false",
                "CANCELLED,CANCELLED,false"
    })
    void taskLifecycleEnforcesAllowedTransitions(String source, String target, boolean allowed) {
        task.setStatus(source);
        UpdateRequest request = new UpdateRequest(null, null, null, null, target, null, 0);
        if (allowed) {
            when(taskMapper.updateById(task)).thenReturn(1);
            assertThat(service.update(9L, request).status()).isEqualTo(target);
            verify(activities).insert(any(TaskActivityEntity.class));
        } else {
            assertThatThrownBy(() -> service.update(9L, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting(ex -> ((BusinessException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.ILLEGAL_STATE_TRANSITION);
            verify(taskMapper, never()).updateById(any(TaskEntity.class));
            verifyNoInteractions(activities, publisher);
        }
    }
}
