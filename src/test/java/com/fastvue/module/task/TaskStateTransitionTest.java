package com.fastvue.module.task;

import com.fastvue.infrastructure.metrics.BusinessMetrics;
import com.fastvue.module.project.service.ProjectService;
import com.fastvue.module.task.api.TaskModels.UpdateRequest;
import com.fastvue.module.task.persistence.*;
import com.fastvue.module.task.service.TaskService;
import com.fastvue.security.LoginUser;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskStateTransitionTest {
    @Mock TaskMapper taskMapper; @Mock TaskCommentMapper comments; @Mock TaskActivityMapper activities;
    @Mock ProjectService projects; @Mock ApplicationEventPublisher publisher; @Mock BusinessMetrics metrics;
    TaskService service; TaskEntity task;

    @BeforeEach void setUp() {
        service = new TaskService(taskMapper, comments, activities, projects, publisher, metrics);
        task = new TaskEntity(); task.setId(9L); task.setProjectId(2L); task.setStatus("IN_PROGRESS"); task.setVersion(0);
        when(taskMapper.selectById(9L)).thenReturn(task);
        LoginUser user = new LoginUser(1L, 1L, "user", "x", true, List.of(), List.of());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void inProgressCanBecomeDone() {
        when(taskMapper.updateById(task)).thenReturn(1);
        service.update(9L, new UpdateRequest(null, null, null, null, "DONE", null, 0));
        assertThat(task.getStatus()).isEqualTo("DONE");
        verify(activities).insert(any(TaskActivityEntity.class));
    }

    @Test void doneTaskCannotBeEditedOrReopened() {
        task.setStatus("DONE");
        assertThatThrownBy(() -> service.update(9L,
                new UpdateRequest("changed", null, null, null, "IN_PROGRESS", null, 0)))
                .hasMessageContaining("不能编辑");
        verify(taskMapper, never()).updateById(any(TaskEntity.class));
    }
}
