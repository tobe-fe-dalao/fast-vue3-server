package com.fastvue.module.approval.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fastvue.common.exception.BusinessException;
import com.fastvue.common.exception.ErrorCode;
import com.fastvue.infrastructure.metrics.BusinessMetrics;
import com.fastvue.infrastructure.tenant.TenantContext;
import com.fastvue.module.approval.api.ApprovalModels.*;
import com.fastvue.module.approval.event.ApprovalFinishedEvent;
import com.fastvue.module.approval.event.ApprovalSubmittedEvent;
import com.fastvue.module.approval.persistence.*;
import com.fastvue.module.organization.persistence.DepartmentEntity;
import com.fastvue.module.organization.persistence.DepartmentMapper;
import com.fastvue.module.user.persistence.UserMapper;
import com.fastvue.security.LoginUser;
import com.fastvue.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApprovalService {
    private final ApprovalRequestMapper requestMapper;
    private final ApprovalStepMapper stepMapper;
    private final ApprovalActionMapper actionMapper;
    private final DepartmentMapper departmentMapper;
    private final UserMapper userMapper;
    private final ApplicationEventPublisher publisher;
    private final BusinessMetrics metrics;

    public List<ApprovalVO> list() {
        LoginUser user = SecurityUtils.currentUser();
        LambdaQueryWrapper<ApprovalRequestEntity> query = new LambdaQueryWrapper<ApprovalRequestEntity>()
                .orderByDesc(ApprovalRequestEntity::getCreatedAt);
        if (!isTenantAdministrator(user)) {
            List<Long> assigned = stepMapper.selectPendingRequestIds(user.id());
            query.and(scope -> {
                scope.eq(ApprovalRequestEntity::getApplicantId, user.id());
                if (!assigned.isEmpty()) scope.or().in(ApprovalRequestEntity::getId, assigned);
            });
        }
        List<ApprovalRequestEntity> requests = requestMapper.selectList(query);
        List<Long> requestIds = requests.stream().map(ApprovalRequestEntity::getId).toList();
        Map<Long, List<ApprovalStepEntity>> stepsByRequest = requestIds.isEmpty() ? Map.of()
                : stepMapper.selectList(new LambdaQueryWrapper<ApprovalStepEntity>()
                        .in(ApprovalStepEntity::getApprovalRequestId, requestIds)
                        .orderByAsc(ApprovalStepEntity::getApprovalRequestId)
                        .orderByAsc(ApprovalStepEntity::getStepOrder)).stream()
                .collect(Collectors.groupingBy(ApprovalStepEntity::getApprovalRequestId));
        return requests.stream()
                .map(value -> toVO(value, stepsByRequest.getOrDefault(value.getId(), List.of())))
                .toList();
    }

    @Transactional
    public ApprovalVO create(CreateRequest request) {
        if (request.departmentId() != null && departmentMapper.selectById(request.departmentId()) == null) {
            throw new BusinessException(ErrorCode.DEPARTMENT_NOT_FOUND);
        }
        ApprovalRequestEntity entity = new ApprovalRequestEntity();
        entity.setTenantId(TenantContext.tenantId());
        entity.setType(request.type());
        entity.setTitle(request.title());
        entity.setBusinessKey(request.businessKey());
        entity.setApplicantId(SecurityUtils.currentUser().id());
        entity.setDepartmentId(request.departmentId());
        entity.setStatus("DRAFT");
        entity.setCurrentStep(0);
        entity.setPayload(request.payload());
        entity.setVersion(0);
        requestMapper.insert(entity);
        return toVO(entity);
    }

    @Transactional
    public ApprovalVO submit(Long id) {
        ApprovalRequestEntity request = requireOwned(id);
        requireStatus(request, "DRAFT");
        if (request.getDepartmentId() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "审批必须选择部门");
        }
        DepartmentEntity department = departmentMapper.selectById(request.getDepartmentId());
        if (department == null || department.getLeaderId() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "部门必须配置负责人后才能提交审批");
        }
        ApprovalStepEntity manager = step(request, 1, "DEPARTMENT_MANAGER", department.getLeaderId(), "PENDING");
        stepMapper.insert(manager);
        stepMapper.insert(step(request, 2, "ADMIN", null, "WAITING"));
        request.setStatus("PENDING");
        request.setCurrentStep(1);
        update(request);
        action(request, manager.getId(), "SUBMIT", null);
        publisher.publishEvent(new ApprovalSubmittedEvent(request.getTenantId(), request.getId(),
                manager.getApproverId(), request.getTitle()));
        metrics.approvalSubmitted();
        return toVO(request);
    }

    @Transactional
    public ApprovalVO approve(Long id, String comment) {
        ApprovalRequestEntity request = require(id);
        requireStatus(request, "PENDING");
        ApprovalStepEntity current = currentStep(request);
        requireApprover(current);
        current.setStatus("APPROVED");
        current.setActedAt(OffsetDateTime.now());
        stepMapper.updateById(current);
        action(request, current.getId(), "APPROVE", comment);
        ApprovalStepEntity next = stepMapper.selectOne(new LambdaQueryWrapper<ApprovalStepEntity>()
                .eq(ApprovalStepEntity::getApprovalRequestId, id)
                .eq(ApprovalStepEntity::getStepOrder, request.getCurrentStep() + 1));
        if (next == null) {
            request.setStatus("APPROVED");
            update(request);
            publisher.publishEvent(new ApprovalFinishedEvent(request.getTenantId(), id,
                    request.getApplicantId(), request.getTitle(), "APPROVED"));
        } else {
            next.setStatus("PENDING");
            stepMapper.updateById(next);
            request.setCurrentStep(next.getStepOrder());
            update(request);
            Long receiver = next.getApproverId();
            if (receiver == null) {
                receiver = administratorFor(request.getTenantId());
            }
            publisher.publishEvent(new ApprovalSubmittedEvent(request.getTenantId(), id, receiver, request.getTitle()));
        }
        return toVO(request);
    }

    @Transactional
    public ApprovalVO reject(Long id, String comment) {
        ApprovalRequestEntity request = require(id);
        requireStatus(request, "PENDING");
        ApprovalStepEntity current = currentStep(request);
        requireApprover(current);
        current.setStatus("REJECTED");
        current.setActedAt(OffsetDateTime.now());
        stepMapper.updateById(current);
        action(request, current.getId(), "REJECT", comment);
        request.setStatus("REJECTED");
        update(request);
        publisher.publishEvent(new ApprovalFinishedEvent(request.getTenantId(), id,
                request.getApplicantId(), request.getTitle(), "REJECTED"));
        metrics.approvalRejected();
        return toVO(request);
    }

    @Transactional
    public ApprovalVO cancel(Long id) {
        ApprovalRequestEntity request = requireOwned(id);
        if (!List.of("DRAFT", "PENDING").contains(request.getStatus())) {
            throw new BusinessException(ErrorCode.ILLEGAL_STATE_TRANSITION);
        }
        request.setStatus("CANCELLED");
        update(request);
        action(request, null, "CANCEL", null);
        return toVO(request);
    }

    private ApprovalStepEntity step(ApprovalRequestEntity request, int order, String type, Long approver, String status) {
        ApprovalStepEntity step = new ApprovalStepEntity();
        step.setTenantId(request.getTenantId()); step.setApprovalRequestId(request.getId());
        step.setStepOrder(order); step.setApproverType(type); step.setApproverId(approver); step.setStatus(status);
        return step;
    }

    private void action(ApprovalRequestEntity request, Long stepId, String name, String comment) {
        ApprovalActionEntity action = new ApprovalActionEntity();
        action.setTenantId(request.getTenantId()); action.setApprovalRequestId(request.getId());
        action.setStepId(stepId); action.setActorId(SecurityUtils.currentUser().id());
        action.setAction(name); action.setComment(comment); action.setCreatedAt(OffsetDateTime.now());
        actionMapper.insert(action);
    }

    private ApprovalRequestEntity require(Long id) {
        ApprovalRequestEntity value = requestMapper.selectById(id);
        if (value == null) throw new BusinessException(ErrorCode.APPROVAL_NOT_FOUND);
        return value;
    }

    private ApprovalRequestEntity requireOwned(Long id) {
        ApprovalRequestEntity value = require(id);
        if (!value.getApplicantId().equals(SecurityUtils.currentUser().id())) throw new BusinessException(ErrorCode.FORBIDDEN);
        return value;
    }

    private void requireStatus(ApprovalRequestEntity request, String status) {
        if (!status.equals(request.getStatus())) throw new BusinessException(ErrorCode.ILLEGAL_STATE_TRANSITION,
                request.getStatus() + " 状态不能执行该操作");
    }

    private ApprovalStepEntity currentStep(ApprovalRequestEntity request) {
        ApprovalStepEntity value = stepMapper.selectOne(new LambdaQueryWrapper<ApprovalStepEntity>()
                .eq(ApprovalStepEntity::getApprovalRequestId, request.getId())
                .eq(ApprovalStepEntity::getStepOrder, request.getCurrentStep()));
        if (value == null || !"PENDING".equals(value.getStatus())) throw new BusinessException(ErrorCode.ILLEGAL_STATE_TRANSITION);
        return value;
    }

    private void requireApprover(ApprovalStepEntity step) {
        LoginUser user = SecurityUtils.currentUser();
        boolean allowed = "ADMIN".equals(step.getApproverType())
                ? isTenantAdministrator(user) : user.id().equals(step.getApproverId());
        if (!allowed) throw new BusinessException(ErrorCode.FORBIDDEN, "当前用户不是该审批步骤的审批人");
    }

    private boolean isTenantAdministrator(LoginUser user) {
        return user.roles().contains("admin") || user.roles().contains("tenant-admin");
    }

    private Long administratorFor(Long tenantId) {
        return TenantContext.runAs(tenantId, false, () -> {
            List<Long> tenantAdministrators = userMapper.selectUserIdsByRoleCode("tenant-admin");
            if (!tenantAdministrators.isEmpty()) return tenantAdministrators.getFirst();
            return tenantId.equals(1L)
                    ? userMapper.selectUserIdsByRoleCode("admin").stream().findFirst().orElse(null)
                    : null;
        });
    }

    private void update(ApprovalRequestEntity request) {
        if (requestMapper.updateById(request) == 0) throw new BusinessException(ErrorCode.OPTIMISTIC_LOCK_CONFLICT);
    }

    private ApprovalVO toVO(ApprovalRequestEntity value) {
        return toVO(value, stepMapper.selectList(new LambdaQueryWrapper<ApprovalStepEntity>()
                .eq(ApprovalStepEntity::getApprovalRequestId, value.getId())
                .orderByAsc(ApprovalStepEntity::getStepOrder)));
    }

    private ApprovalVO toVO(ApprovalRequestEntity value, List<ApprovalStepEntity> stepEntities) {
        List<StepVO> steps = stepEntities.stream()
                .map(v -> new StepVO(v.getId(), v.getStepOrder(), v.getApproverType(), v.getApproverId(),
                        v.getStatus(), v.getActedAt())).toList();
        return new ApprovalVO(value.getId(), value.getType(), value.getTitle(), value.getBusinessKey(),
                value.getApplicantId(), value.getDepartmentId(), value.getStatus(), value.getCurrentStep(),
                value.getPayload(), value.getVersion(), steps, value.getCreatedAt(), value.getUpdatedAt());
    }
}
