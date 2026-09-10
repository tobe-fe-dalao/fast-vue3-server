package com.fastvue.module.role.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fastvue.common.exception.BusinessException;
import com.fastvue.common.exception.ErrorCode;
import com.fastvue.module.role.api.CreateRoleRequest;
import com.fastvue.module.role.api.RoleQueryRequest;
import com.fastvue.module.role.api.RoleVO;
import com.fastvue.module.role.api.UpdateRoleRequest;
import com.fastvue.module.role.persistence.RoleEntity;
import com.fastvue.module.role.persistence.RoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.fastvue.security.PermissionCache;
import org.springframework.beans.factory.annotation.Autowired;
import com.fastvue.infrastructure.tenant.TenantContext;
import com.fastvue.security.SecurityUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 角色业务逻辑。
 */
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleMapper roleMapper;
    private final RoleConverter roleConverter;

    @Autowired(required = false)
    private PermissionCache permissionCache;

    /**
     * 分页查询角色。
     */
    public Page<RoleVO> page(RoleQueryRequest query) {
        LambdaQueryWrapper<RoleEntity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.keyword())) {
            wrapper.and(w -> w.like(RoleEntity::getName, query.keyword())
                    .or()
                    .like(RoleEntity::getCode, query.keyword()));
        }
        wrapper.orderByAsc(RoleEntity::getId);

        Page<RoleEntity> page = roleMapper.selectPage(new Page<>(query.page(), query.pageSize()), wrapper);
        Page<RoleVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<Long> roleIds = page.getRecords().stream().map(RoleEntity::getId).toList();
        Map<Long, List<String>> permissions = roleIds.isEmpty() ? Map.of()
                : roleMapper.selectPermissionRows(roleIds).stream().collect(Collectors.groupingBy(
                        RoleMapper.RolePermissionRow::roleId,
                        Collectors.mapping(RoleMapper.RolePermissionRow::code, Collectors.toList())));
        Map<Long, List<Long>> menus = roleIds.isEmpty() ? Map.of()
                : roleMapper.selectMenuRows(roleIds).stream().collect(Collectors.groupingBy(
                        RoleMapper.RoleMenuRow::roleId,
                        Collectors.mapping(RoleMapper.RoleMenuRow::menuId, Collectors.toList())));
        result.setRecords(page.getRecords().stream()
                .map(entity -> toVO(entity, permissions.getOrDefault(entity.getId(), List.of()),
                        menus.getOrDefault(entity.getId(), List.of())))
                .toList());
        return result;
    }

    /**
     * 查询单个角色。
     */
    public RoleVO getById(Long id) {
        RoleEntity entity = roleMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }
        return toVO(entity);
    }

    /**
     * 创建角色。
     */
    @Transactional
    public RoleVO create(CreateRoleRequest request) {
        if (List.of("admin", "tenant-admin").contains(request.code())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "保留的管理员角色编码不可创建");
        }
        if (existsByCode(request.code())) {
            throw new BusinessException(ErrorCode.ROLE_ALREADY_EXISTS);
        }

        RoleEntity entity = new RoleEntity();
        entity.setTenantId(TenantContext.tenantId());
        entity.setCode(request.code());
        entity.setName(request.name());
        entity.setDescription(request.description());
        roleMapper.insert(entity);

        bindRelations(entity.getId(), request.permissionIds(), request.menuIds());
        invalidatePermissions();
        return getById(entity.getId());
    }

    /**
     * 更新角色。
     */
    @Transactional
    public RoleVO update(Long id, UpdateRoleRequest request) {
        RoleEntity entity = roleMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }
        requireSystemRoleOwner(entity);

        if (request.name() != null) {
            entity.setName(request.name());
        }
        if (request.description() != null) {
            entity.setDescription(request.description());
        }
        roleMapper.updateById(entity);

        if (request.permissionIds() != null) {
            roleMapper.deleteRolePermissions(id);
            if (!request.permissionIds().isEmpty()) {
                roleMapper.insertRolePermissions(entity.getTenantId(), id, request.permissionIds());
            }
        }
        if (request.menuIds() != null) {
            roleMapper.deleteRoleMenus(id);
            if (!request.menuIds().isEmpty()) {
                roleMapper.insertRoleMenus(entity.getTenantId(), id, request.menuIds());
            }
        }
        invalidatePermissions();

        return getById(id);
    }

    /**
     * 删除角色（逻辑删除），并清理关联。
     */
    @Transactional
    public void delete(Long id) {
        RoleEntity entity = roleMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }
        if (List.of("admin", "tenant-admin").contains(entity.getCode())) {
            throw new BusinessException(ErrorCode.CONFLICT, "管理员角色不可删除");
        }
        roleMapper.deleteById(id);
        roleMapper.deleteRolePermissions(id);
        roleMapper.deleteRoleMenus(id);
        invalidatePermissions();
    }

    private void bindRelations(Long roleId, java.util.List<Long> permissionIds, java.util.List<Long> menuIds) {
        if (permissionIds != null && !permissionIds.isEmpty()) {
            roleMapper.insertRolePermissions(TenantContext.tenantId(), roleId, permissionIds);
        }
        if (menuIds != null && !menuIds.isEmpty()) {
            roleMapper.insertRoleMenus(TenantContext.tenantId(), roleId, menuIds);
        }
    }

    private RoleVO toVO(RoleEntity entity) {
        return toVO(entity, roleMapper.selectPermissionCodes(entity.getId()), roleMapper.selectMenuIds(entity.getId()));
    }

    private RoleVO toVO(RoleEntity entity, List<String> permissionCodes, List<Long> menuIds) {
        RoleVO vo = roleConverter.toVO(entity);
        return new RoleVO(
                vo.id(), vo.code(), vo.name(), vo.description(),
                permissionCodes, menuIds,
                vo.createdAt(), vo.updatedAt());
    }

    private boolean existsByCode(String code) {
        return roleMapper.selectCount(
                new LambdaQueryWrapper<RoleEntity>()
                        .eq(RoleEntity::getTenantId, TenantContext.tenantId())
                        .eq(RoleEntity::getCode, code)) > 0;
    }

    private void invalidatePermissions() {
        if (permissionCache != null) permissionCache.invalidateAll();
    }

    private void requireSystemRoleOwner(RoleEntity entity) {
        if (entity.getId().equals(1L) && !TenantContext.isSuperAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "系统管理员角色只能由系统管理员修改");
        }
        if ("tenant-admin".equals(entity.getCode()) && !TenantContext.isSuperAdmin()
                && (SecurityUtils.currentUserOrNull() == null
                || !SecurityUtils.currentUserOrNull().roles().contains("tenant-admin"))) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "租户管理员角色只能由租户管理员修改");
        }
    }
}
