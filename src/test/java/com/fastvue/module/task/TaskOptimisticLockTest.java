package com.fastvue.module.task;

import com.fastvue.common.exception.BusinessException;
import com.fastvue.common.exception.ErrorCode;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskOptimisticLockTest {
    @Mock TaskMapper taskMapper; @Mock TaskCommentMapper comments; @Mock TaskActivityMapper activities;
    @Mock ProjectService projects; @Mock ApplicationEventPublisher publisher; @Mock BusinessMetrics metrics;

    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void staleWriterReceivesConflict() {
        TaskService service = new TaskService(taskMapper, comments, activities, projects, publisher, metrics);
        TaskEntity task = new TaskEntity(); task.setId(3L); task.setProjectId(2L); task.setStatus("TODO"); task.setVersion(2);
        when(taskMapper.selectById(3L)).thenReturn(task);
        when(taskMapper.updateById(task)).thenReturn(0);
        LoginUser user = new LoginUser(1L, 1L, "user", "x", true, List.of(), List.of());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));

        assertThatThrownBy(() -> service.update(3L,
                new UpdateRequest("new", null, null, null, null, null, 1)))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.OPTIMISTIC_LOCK_CONFLICT);
    }
}
