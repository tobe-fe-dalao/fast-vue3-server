package com.fastvue.module.task.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fastvue.common.exception.BusinessException;
import com.fastvue.common.exception.ErrorCode;
import com.fastvue.infrastructure.metrics.BusinessMetrics;
import com.fastvue.infrastructure.tenant.TenantContext;
import com.fastvue.module.project.persistence.ProjectEntity;
import com.fastvue.module.project.service.ProjectService;
import com.fastvue.module.task.api.TaskModels.*;
import com.fastvue.module.task.event.TaskAssignedEvent;
import com.fastvue.module.task.persistence.*;
import com.fastvue.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TaskService {
    private static final Map<String, Set<String>> TRANSITIONS = Map.of(
            "TODO", Set.of("IN_PROGRESS", "CANCELLED"),
            "IN_PROGRESS", Set.of("BLOCKED", "DONE", "CANCELLED"),
            "BLOCKED", Set.of("IN_PROGRESS", "CANCELLED"),
            "DONE", Set.of(),
            "CANCELLED", Set.of());

    private final TaskMapper taskMapper;
    private final TaskCommentMapper commentMapper;
    private final TaskActivityMapper activityMapper;
    private final ProjectService projectService;
    private final ApplicationEventPublisher eventPublisher;
    private final BusinessMetrics metrics;

    public List<TaskVO> list(Long projectId) {
        projectService.requireAccessible(projectId);
        return taskMapper.selectList(new LambdaQueryWrapper<TaskEntity>()
                .eq(TaskEntity::getProjectId, projectId).orderByDesc(TaskEntity::getCreatedAt))
                .stream().map(this::toVO).toList();
    }

    public TaskVO get(Long id) { return toVO(requireAccessible(id)); }

    @Transactional
    public TaskVO create(Long projectId, CreateRequest request) {
        ProjectEntity project = projectService.requireAccessible(projectId);
        if ("ARCHIVED".equals(project.getStatus())) {
            throw new BusinessException(ErrorCode.ILLEGAL_STATE_TRANSITION, "归档项目不能新增任务");
        }
        if (request.assigneeId() != null) projectService.requireMember(projectId, request.assigneeId());
        TaskEntity entity = new TaskEntity();
        entity.setTenantId(project.getTenantId());
        entity.setProjectId(projectId);
        entity.setTitle(request.title());
        entity.setDescription(request.description());
        entity.setAssigneeId(request.assigneeId());
        entity.setReporterId(SecurityUtils.currentUser().id());
        entity.setPriority(request.priority() == null ? "MEDIUM" : request.priority());
        entity.setStatus("TODO");
        entity.setDueDate(request.dueDate());
        entity.setVersion(0);
        taskMapper.insert(entity);
        activity(entity.getTenantId(), entity.getId(), "TASK_CREATED", null, null, entity.getTitle());
        if (entity.getAssigneeId() != null) publishAssigned(entity);
        metrics.taskCreated();
        return toVO(entity);
    }

    @Transactional
    public TaskVO update(Long id, UpdateRequest request) {
        TaskEntity entity = requireAccessible(id);
        if ("DONE".equals(entity.getStatus()) || "CANCELLED".equals(entity.getStatus())) {
            throw new BusinessException(ErrorCode.ILLEGAL_STATE_TRANSITION, "已完成或已取消任务不能编辑关键字段");
        }
        entity.setVersion(request.version());
        change(entity, "title", entity.getTitle(), request.title(), entity::setTitle);
        change(entity, "description", entity.getDescription(), request.description(), entity::setDescription);
        if (request.assigneeId() != null && !Objects.equals(request.assigneeId(), entity.getAssigneeId())) {
            projectService.requireMember(entity.getProjectId(), request.assigneeId());
            Long old = entity.getAssigneeId();
            entity.setAssigneeId(request.assigneeId());
            activity(entity.getTenantId(), entity.getId(), "TASK_ASSIGNED", "assigneeId", string(old), string(request.assigneeId()));
            publishAssigned(entity);
        }
        change(entity, "priority", entity.getPriority(), request.priority(), entity::setPriority);
        if (request.dueDate() != null && !Objects.equals(request.dueDate(), entity.getDueDate())) {
            activity(entity.getTenantId(), entity.getId(), "FIELD_CHANGED", "dueDate", string(entity.getDueDate()), string(request.dueDate()));
            entity.setDueDate(request.dueDate());
        }
        if (request.status() != null && !request.status().equals(entity.getStatus())) {
            transition(entity, request.status());
        }
        if (taskMapper.updateById(entity) == 0) throw new BusinessException(ErrorCode.OPTIMISTIC_LOCK_CONFLICT);
        return toVO(entity);
    }

    @Transactional
    public CommentVO comment(Long taskId, CommentRequest request) {
        TaskCommentEntity comment = new TaskCommentEntity();
        TaskEntity task = requireAccessible(taskId);
        comment.setTenantId(task.getTenantId());
        comment.setTaskId(taskId);
        comment.setAuthorId(SecurityUtils.currentUser().id());
        comment.setContent(request.content().trim());
        commentMapper.insert(comment);
        activity(task.getTenantId(), taskId, "COMMENT_ADDED", null, null, String.valueOf(comment.getId()));
        return new CommentVO(comment.getId(), taskId, comment.getAuthorId(), comment.getContent(), comment.getCreatedAt());
    }

    public List<CommentVO> comments(Long taskId) {
        requireAccessible(taskId);
        return commentMapper.selectList(new LambdaQueryWrapper<TaskCommentEntity>()
                .eq(TaskCommentEntity::getTaskId, taskId).orderByAsc(TaskCommentEntity::getCreatedAt))
                .stream().map(v -> new CommentVO(v.getId(), v.getTaskId(), v.getAuthorId(), v.getContent(), v.getCreatedAt())).toList();
    }

    public List<ActivityVO> activities(Long taskId) {
        requireAccessible(taskId);
        return activityMapper.selectList(new LambdaQueryWrapper<TaskActivityEntity>()
                .eq(TaskActivityEntity::getTaskId, taskId).orderByDesc(TaskActivityEntity::getCreatedAt))
                .stream().map(v -> new ActivityVO(v.getId(), v.getTaskId(), v.getActorId(), v.getAction(),
                        v.getFieldName(), v.getOldValue(), v.getNewValue(), v.getCreatedAt())).toList();
    }

    private void transition(TaskEntity entity, String target) {
        if (!TRANSITIONS.getOrDefault(entity.getStatus(), Set.of()).contains(target)) {
            throw new BusinessException(ErrorCode.ILLEGAL_STATE_TRANSITION,
                    entity.getStatus() + " 不能转换为 " + target);
        }
        String old = entity.getStatus();
        entity.setStatus(target);
        activity(entity.getTenantId(), entity.getId(), "STATUS_CHANGED", "status", old, target);
    }

    private void change(TaskEntity task, String field, String oldValue, String newValue,
                        java.util.function.Consumer<String> setter) {
        if (newValue != null && !Objects.equals(oldValue, newValue)) {
            setter.accept(newValue);
            activity(task.getTenantId(), task.getId(), "FIELD_CHANGED", field, oldValue, newValue);
        }
    }

    private void activity(Long tenantId, Long taskId, String action, String field, String oldValue, String newValue) {
        TaskActivityEntity activity = new TaskActivityEntity();
        activity.setTenantId(tenantId);
        activity.setTaskId(taskId);
        activity.setActorId(SecurityUtils.currentUser().id());
        activity.setAction(action);
        activity.setFieldName(field);
        activity.setOldValue(oldValue);
        activity.setNewValue(newValue);
        activity.setCreatedAt(OffsetDateTime.now());
        activityMapper.insert(activity);
    }

    private TaskEntity requireAccessible(Long id) {
        TaskEntity task = taskMapper.selectById(id);
        if (task == null) throw new BusinessException(ErrorCode.TASK_NOT_FOUND);
        projectService.requireAccessible(task.getProjectId());
        return task;
    }

    private void publishAssigned(TaskEntity task) {
        eventPublisher.publishEvent(new TaskAssignedEvent(task.getTenantId(), task.getId(),
                task.getAssigneeId(), task.getTitle()));
    }

    private String string(Object value) { return value == null ? null : String.valueOf(value); }

    private TaskVO toVO(TaskEntity value) {
        return new TaskVO(value.getId(), value.getProjectId(), value.getTitle(), value.getDescription(),
                value.getAssigneeId(), value.getReporterId(), value.getPriority(), value.getStatus(),
                value.getDueDate(), value.getVersion(), value.getCreatedAt(), value.getUpdatedAt());
    }
}
