package com.fastvue.module.organization.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fastvue.common.exception.BusinessException;
import com.fastvue.common.exception.ErrorCode;
import com.fastvue.infrastructure.tenant.TenantContext;
import com.fastvue.module.organization.api.DepartmentModels.DepartmentVO;
import com.fastvue.module.organization.api.DepartmentModels.MemberVO;
import com.fastvue.module.organization.api.DepartmentModels.SaveRequest;
import com.fastvue.module.organization.persistence.DepartmentEntity;
import com.fastvue.module.organization.persistence.DepartmentMapper;
import com.fastvue.module.organization.persistence.OrganizationEntity;
import com.fastvue.module.organization.persistence.OrganizationMapper;
import com.fastvue.module.user.persistence.UserEntity;
import com.fastvue.module.user.persistence.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DepartmentService {
    private final DepartmentMapper departmentMapper;
    private final OrganizationMapper organizationMapper;
    private final UserMapper userMapper;

    public List<DepartmentVO> tree() {
        List<DepartmentEntity> all = departmentMapper.selectList(new LambdaQueryWrapper<DepartmentEntity>()
                .orderByAsc(DepartmentEntity::getSort).orderByAsc(DepartmentEntity::getId));
        return buildTree(all, null);
    }

    public List<MemberVO> members(Long departmentId) {
        requireDepartment(departmentId);
        List<Long> ids = departmentMapper.selectMemberIds(departmentId);
        if (ids.isEmpty()) return List.of();
        return userMapper.selectBatchIds(ids).stream()
                .map(user -> new MemberVO(user.getId(), user.getUsername(), user.getNickname(), user.getStatus()))
                .toList();
    }

    @Transactional
    public DepartmentVO create(SaveRequest request) {
        validateUniqueCode(request.code(), null, TenantContext.tenantId());
        validateLeader(request.leaderId(), TenantContext.tenantId());
        DepartmentEntity parent = validateParent(null, request.parentId());
        OrganizationEntity organization = organizationMapper.selectOne(new LambdaQueryWrapper<OrganizationEntity>()
                .eq(OrganizationEntity::getTenantId, TenantContext.tenantId()));
        if (organization == null) throw new BusinessException(ErrorCode.NOT_FOUND, "组织不存在");
        DepartmentEntity entity = new DepartmentEntity();
        entity.setTenantId(TenantContext.tenantId());
        entity.setOrganizationId(parent == null ? organization.getId() : parent.getOrganizationId());
        apply(entity, request);
        departmentMapper.insert(entity);
        assignLeaderMembership(entity, null);
        return toVO(entity, List.of());
    }

    @Transactional
    public DepartmentVO update(Long id, SaveRequest request) {
        DepartmentEntity entity = requireDepartment(id);
        Long previousLeaderId = entity.getLeaderId();
        validateUniqueCode(request.code(), id, entity.getTenantId());
        validateLeader(request.leaderId(), entity.getTenantId());
        DepartmentEntity parent = validateParent(id, request.parentId());
        if (parent != null && !parent.getOrganizationId().equals(entity.getOrganizationId())) {
            throw new BusinessException(ErrorCode.CONFLICT, "不能跨组织移动部门");
        }
        apply(entity, request);
        departmentMapper.updateById(entity);
        assignLeaderMembership(entity, previousLeaderId);
        return toVO(entity, List.of());
    }

    @Transactional
    public void assignMember(Long departmentId, Long userId) {
        DepartmentEntity department = requireDepartment(departmentId);
        UserEntity user = TenantContext.runAs(department.getTenantId(), false, () -> userMapper.selectById(userId));
        if (user == null) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        departmentMapper.removeUserMembership(userId);
        departmentMapper.addMember(department.getTenantId(), departmentId, userId);
    }

    @Transactional
    public void removeMember(Long departmentId, Long userId) {
        DepartmentEntity department = requireDepartment(departmentId);
        if (userId.equals(department.getLeaderId())) {
            throw new BusinessException(ErrorCode.CONFLICT, "请先更换部门负责人");
        }
        departmentMapper.removeMember(departmentId, userId);
    }

    @Transactional
    public void delete(Long id) {
        requireDepartment(id);
        if (departmentMapper.selectCount(new LambdaQueryWrapper<DepartmentEntity>()
                .eq(DepartmentEntity::getParentId, id)) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "存在子部门，无法删除");
        }
        if (departmentMapper.countMembers(id) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "部门仍有成员，无法删除");
        }
        departmentMapper.deleteById(id);
    }

    private void apply(DepartmentEntity entity, SaveRequest request) {
        entity.setParentId(request.parentId());
        entity.setName(request.name());
        entity.setCode(request.code());
        entity.setLeaderId(request.leaderId());
        entity.setSort(request.sort() == null ? 0 : request.sort());
        entity.setStatus(request.status() == null ? "active" : request.status());
    }

    private DepartmentEntity validateParent(Long id, Long parentId) {
        if (parentId == null) return null;
        if (parentId.equals(id)) throw new BusinessException(ErrorCode.CONFLICT, "部门不能成为自己的父部门");
        DepartmentEntity parent = requireDepartment(parentId);
        HashSet<Long> visited = new HashSet<>();
        while (parent != null) {
            if (!visited.add(parent.getId()) || parent.getId().equals(id)) {
                throw new BusinessException(ErrorCode.CONFLICT, "部门层级不能形成循环");
            }
            parent = parent.getParentId() == null ? null : requireDepartment(parent.getParentId());
        }
        return requireDepartment(parentId);
    }

    private void validateLeader(Long leaderId, Long tenantId) {
        if (leaderId != null && TenantContext.runAs(tenantId, false, () -> userMapper.selectById(leaderId)) == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "部门负责人不存在或不属于当前租户");
        }
    }

    private void assignLeaderMembership(DepartmentEntity department, Long previousLeaderId) {
        Long leaderId = department.getLeaderId();
        if (leaderId == null || leaderId.equals(previousLeaderId)) return;
        departmentMapper.removeUserMembership(leaderId);
        departmentMapper.addMember(department.getTenantId(), department.getId(), leaderId);
    }

    private void validateUniqueCode(String code, Long excludeId, Long tenantId) {
        LambdaQueryWrapper<DepartmentEntity> query = new LambdaQueryWrapper<DepartmentEntity>()
                .eq(DepartmentEntity::getTenantId, tenantId)
                .eq(DepartmentEntity::getCode, code);
        if (excludeId != null) query.ne(DepartmentEntity::getId, excludeId);
        if (departmentMapper.selectCount(query) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "部门编码已存在");
        }
    }

    private DepartmentEntity requireDepartment(Long id) {
        DepartmentEntity value = departmentMapper.selectById(id);
        if (value == null) throw new BusinessException(ErrorCode.DEPARTMENT_NOT_FOUND);
        return value;
    }

    private List<DepartmentVO> buildTree(List<DepartmentEntity> all, Long parentId) {
        Map<Long, List<DepartmentEntity>> byParent = all.stream()
                .collect(Collectors.groupingBy(value -> value.getParentId() == null ? 0L : value.getParentId()));
        long key = parentId == null ? 0L : parentId;
        return byParent.getOrDefault(key, List.of()).stream()
                .sorted(Comparator.comparing(DepartmentEntity::getSort).thenComparing(DepartmentEntity::getId))
                .map(value -> toVO(value, buildTree(all, value.getId())))
                .toList();
    }

    private DepartmentVO toVO(DepartmentEntity value, List<DepartmentVO> children) {
        return new DepartmentVO(value.getId(), value.getParentId(), value.getName(), value.getCode(),
                value.getLeaderId(), value.getSort(), value.getStatus(), children);
    }
}
