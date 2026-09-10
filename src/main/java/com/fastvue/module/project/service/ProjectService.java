package com.fastvue.module.project.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fastvue.common.exception.BusinessException;
import com.fastvue.common.exception.ErrorCode;
import com.fastvue.infrastructure.metrics.BusinessMetrics;
import com.fastvue.infrastructure.tenant.TenantContext;
import com.fastvue.module.project.api.ProjectModels.CreateRequest;
import com.fastvue.module.project.api.ProjectModels.ProjectVO;
import com.fastvue.module.project.api.ProjectModels.UpdateRequest;
import com.fastvue.module.project.persistence.ProjectEntity;
import com.fastvue.module.project.persistence.ProjectMapper;
import com.fastvue.module.project.persistence.ProjectActivityEntity;
import com.fastvue.module.project.persistence.ProjectActivityMapper;
import com.fastvue.module.user.persistence.UserMapper;
import com.fastvue.module.task.persistence.TaskEntity;
import com.fastvue.module.task.persistence.TaskMapper;
import com.fastvue.security.LoginUser;
import com.fastvue.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class ProjectService {
    private final ProjectMapper projectMapper;
    private final UserMapper userMapper;
    private final BusinessMetrics metrics;
    private final ProjectActivityMapper activityMapper;
    private final TaskMapper taskMapper;

    public List<ProjectVO> list() {
        LoginUser user = SecurityUtils.currentUser();
        LambdaQueryWrapper<ProjectEntity> query = new LambdaQueryWrapper<ProjectEntity>()
                .orderByDesc(ProjectEntity::getCreatedAt);
        if (!TenantContext.isSuperAdmin()) {
            List<Long> memberProjectIds = projectIdsForMember(user.id());
            if (memberProjectIds.isEmpty()) return List.of();
            query.in(ProjectEntity::getId, memberProjectIds);
        }
        List<ProjectEntity> projects = projectMapper.selectList(query);
        List<Long> ids = projects.stream().map(ProjectEntity::getId).toList();
        Map<Long, List<Long>> members = ids.isEmpty() ? Map.of() : projectMapper.selectMemberRows(ids).stream()
                .collect(Collectors.groupingBy(ProjectMapper.ProjectMemberRow::projectId,
                        Collectors.mapping(ProjectMapper.ProjectMemberRow::userId, Collectors.toList())));
        return projects.stream().map(value -> toVO(value, members.getOrDefault(value.getId(), List.of()))).toList();
    }

    public ProjectVO get(Long id) {
        return toVO(requireAccessible(id));
    }

    public List<com.fastvue.module.project.api.ProjectModels.ActivityVO> activities(Long id) {
        requireAccessible(id);
        return activityMapper.selectList(new LambdaQueryWrapper<ProjectActivityEntity>()
                .eq(ProjectActivityEntity::getProjectId, id)
                .orderByDesc(ProjectActivityEntity::getCreatedAt)).stream()
                .map(value -> new com.fastvue.module.project.api.ProjectModels.ActivityVO(value.getId(),
                        value.getProjectId(), value.getActorId(), value.getAction(), value.getDetail(), value.getCreatedAt()))
                .toList();
    }

    @Transactional
    public ProjectVO create(CreateRequest request) {
        if (request.startDate() != null && request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "项目结束日期不能早于开始日期");
        }
        if (projectMapper.selectCount(new LambdaQueryWrapper<ProjectEntity>()
                .eq(ProjectEntity::getTenantId, TenantContext.tenantId())
                .eq(ProjectEntity::getCode, request.code())) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "项目编码已存在");
        }
        LoginUser user = SecurityUtils.currentUser();
        ProjectEntity entity = new ProjectEntity();
        entity.setTenantId(user.tenantId());
        entity.setName(request.name());
        entity.setCode(request.code());
        entity.setDescription(request.description());
        entity.setOwnerId(user.id());
        entity.setStatus("ACTIVE");
        entity.setStartDate(request.startDate());
        entity.setEndDate(request.endDate());
        entity.setVersion(0);
        projectMapper.insert(entity);
        projectMapper.addMember(user.tenantId(), entity.getId(), user.id(), "OWNER");
        activity(entity.getTenantId(), entity.getId(), "PROJECT_CREATED", entity.getName());
        metrics.projectCreated();
        return toVO(entity);
    }

    @Transactional
    public ProjectVO update(Long id, UpdateRequest request) {
        ProjectEntity entity = requireAccessible(id);
        if ("ARCHIVED".equals(entity.getStatus())) {
            throw new BusinessException(ErrorCode.ILLEGAL_STATE_TRANSITION, "归档项目不能编辑");
        }
        entity.setVersion(request.version());
        if (request.name() != null) entity.setName(request.name());
        if (request.description() != null) entity.setDescription(request.description());
        if (request.startDate() != null) entity.setStartDate(request.startDate());
        if (request.endDate() != null) entity.setEndDate(request.endDate());
        if (entity.getStartDate() != null && entity.getEndDate() != null
                && entity.getEndDate().isBefore(entity.getStartDate())) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "项目结束日期不能早于开始日期");
        }
        if (projectMapper.updateById(entity) == 0) throw new BusinessException(ErrorCode.OPTIMISTIC_LOCK_CONFLICT);
        activity(entity.getTenantId(), entity.getId(), "PROJECT_UPDATED", entity.getName());
        return toVO(entity);
    }

    @Transactional
    public ProjectVO archive(Long id, Integer version) {
        ProjectEntity entity = requireAccessible(id);
        if ("ARCHIVED".equals(entity.getStatus())) return toVO(entity);
        entity.setStatus("ARCHIVED");
        entity.setVersion(version);
        if (projectMapper.updateById(entity) == 0) throw new BusinessException(ErrorCode.OPTIMISTIC_LOCK_CONFLICT);
        activity(entity.getTenantId(), entity.getId(), "PROJECT_ARCHIVED", entity.getName());
        return toVO(entity);
    }

    @Transactional
    public void addMember(Long projectId, Long userId) {
        ProjectEntity project = requireAccessible(projectId);
        if ("ARCHIVED".equals(project.getStatus())) throw new BusinessException(ErrorCode.CONFLICT, "归档项目不能变更成员");
        if (TenantContext.runAs(project.getTenantId(), false, () -> userMapper.selectById(userId)) == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        projectMapper.addMember(project.getTenantId(), projectId, userId, "MEMBER");
        activity(project.getTenantId(), projectId, "PROJECT_MEMBER_ADDED", String.valueOf(userId));
    }

    @Transactional
    public void removeMember(Long projectId, Long userId) {
        ProjectEntity project = requireAccessible(projectId);
        if ("ARCHIVED".equals(project.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "归档项目不能变更成员");
        }
        if (project.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.CONFLICT, "项目负责人不能被移出项目");
        }
        long activeAssignments = taskMapper.selectCount(new LambdaQueryWrapper<TaskEntity>()
                .eq(TaskEntity::getProjectId, projectId)
                .eq(TaskEntity::getAssigneeId, userId)
                .notIn(TaskEntity::getStatus, List.of("DONE", "CANCELLED")));
        if (activeAssignments > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "该成员仍有未完成任务，不能移出项目");
        }
        projectMapper.removeMember(projectId, userId);
        activity(project.getTenantId(), projectId, "PROJECT_MEMBER_REMOVED", String.valueOf(userId));
    }

    public ProjectEntity requireAccessible(Long id) {
        ProjectEntity project = projectMapper.selectById(id);
        if (project == null) throw new BusinessException(ErrorCode.PROJECT_NOT_FOUND);
        LoginUser user = SecurityUtils.currentUser();
        if (!TenantContext.isSuperAdmin() && !projectMapper.isMember(id, user.id())) {
            throw new BusinessException(ErrorCode.PROJECT_NOT_FOUND);
        }
        return project;
    }

    public void requireMember(Long projectId, Long userId) {
        if (!projectMapper.isMember(projectId, userId)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "用户不是项目成员");
        }
    }

    private void activity(Long tenantId, Long projectId, String action, String detail) {
        ProjectActivityEntity value = new ProjectActivityEntity();
        value.setTenantId(tenantId); value.setProjectId(projectId);
        value.setActorId(SecurityUtils.currentUser().id()); value.setAction(action);
        value.setDetail(detail); value.setCreatedAt(OffsetDateTime.now());
        activityMapper.insert(value);
    }

    private List<Long> projectIdsForMember(Long userId) {
        return projectMapper.selectProjectIdsForMember(userId);
    }

    private ProjectVO toVO(ProjectEntity value) {
        return toVO(value, projectMapper.selectMemberIds(value.getId()));
    }

    private ProjectVO toVO(ProjectEntity value, List<Long> memberIds) {
        return new ProjectVO(value.getId(), value.getName(), value.getCode(), value.getDescription(),
                value.getOwnerId(), value.getStatus(), value.getStartDate(), value.getEndDate(), value.getVersion(),
                memberIds, value.getCreatedAt(), value.getUpdatedAt());
    }
}
