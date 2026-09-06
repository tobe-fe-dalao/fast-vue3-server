package com.fastvue.module.role;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fastvue.common.BusinessException;
import com.fastvue.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 角色业务逻辑。
 */
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleMapper roleMapper;
    private final RoleConverter roleConverter;

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
        result.setRecords(page.getRecords().stream()
                .map(this::toVO)
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
        if (existsByCode(request.code())) {
            throw new BusinessException(ErrorCode.ROLE_ALREADY_EXISTS);
        }

        RoleEntity entity = new RoleEntity();
        entity.setCode(request.code());
        entity.setName(request.name());
        entity.setDescription(request.description());
        roleMapper.insert(entity);

        bindRelations(entity.getId(), request.permissionIds(), request.menuIds());
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
                roleMapper.insertRolePermissions(id, request.permissionIds());
            }
        }
        if (request.menuIds() != null) {
            roleMapper.deleteRoleMenus(id);
            if (!request.menuIds().isEmpty()) {
                roleMapper.insertRoleMenus(id, request.menuIds());
            }
        }

        return getById(id);
    }

    /**
     * 删除角色（逻辑删除），并清理关联。
     */
    @Transactional
    public void delete(Long id) {
        if (roleMapper.selectById(id) == null) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }
        roleMapper.deleteById(id);
        roleMapper.deleteRolePermissions(id);
        roleMapper.deleteRoleMenus(id);
    }

    private void bindRelations(Long roleId, java.util.List<Long> permissionIds, java.util.List<Long> menuIds) {
        if (permissionIds != null && !permissionIds.isEmpty()) {
            roleMapper.insertRolePermissions(roleId, permissionIds);
        }
        if (menuIds != null && !menuIds.isEmpty()) {
            roleMapper.insertRoleMenus(roleId, menuIds);
        }
    }

    private RoleVO toVO(RoleEntity entity) {
        RoleVO vo = roleConverter.toVO(entity);
        return new RoleVO(
                vo.id(), vo.code(), vo.name(), vo.description(),
                roleMapper.selectPermissionCodes(entity.getId()),
                roleMapper.selectMenuIds(entity.getId()),
                vo.createdAt(), vo.updatedAt());
    }

    private boolean existsByCode(String code) {
        return roleMapper.selectCount(
                new LambdaQueryWrapper<RoleEntity>().eq(RoleEntity::getCode, code)) > 0;
    }
}
