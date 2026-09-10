package com.fastvue.module.permission.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fastvue.common.exception.BusinessException;
import com.fastvue.common.exception.ErrorCode;
import com.fastvue.module.permission.api.PermissionRequest;
import com.fastvue.module.permission.api.PermissionVO;
import com.fastvue.module.permission.persistence.PermissionEntity;
import com.fastvue.module.permission.persistence.PermissionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import com.fastvue.security.PermissionCache;
import org.springframework.beans.factory.annotation.Autowired;
import com.fastvue.infrastructure.tenant.TenantContext;

/**
 * 权限业务逻辑。
 */
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionMapper permissionMapper;
    private final PermissionConverter permissionConverter;

    @Autowired(required = false)
    private PermissionCache permissionCache;

    /**
     * 查询全部权限。
     */
    public List<PermissionVO> list() {
        return permissionMapper.selectList(
                        new LambdaQueryWrapper<PermissionEntity>().orderByAsc(PermissionEntity::getId))
                .stream()
                .map(permissionConverter::toVO)
                .toList();
    }

    /**
     * 创建权限。
     */
    public PermissionVO create(PermissionRequest request) {
        requireSystemAdministrator();
        if (existsByCode(request.code())) {
            throw new BusinessException(ErrorCode.CONFLICT, "权限编码已存在");
        }
        PermissionEntity entity = new PermissionEntity();
        entity.setCode(request.code());
        entity.setName(request.name());
        entity.setDescription(request.description());
        permissionMapper.insert(entity);
        invalidatePermissions();
        return permissionConverter.toVO(entity);
    }

    /**
     * 更新权限。
     */
    public PermissionVO update(Long id, PermissionRequest request) {
        requireSystemAdministrator();
        PermissionEntity entity = permissionMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.PERMISSION_NOT_FOUND);
        }
        entity.setName(request.name());
        entity.setDescription(request.description());
        permissionMapper.updateById(entity);
        invalidatePermissions();
        return permissionConverter.toVO(entity);
    }

    /**
     * 删除权限。
     */
    public void delete(Long id) {
        requireSystemAdministrator();
        if (permissionMapper.selectById(id) == null) {
            throw new BusinessException(ErrorCode.PERMISSION_NOT_FOUND);
        }
        permissionMapper.deleteById(id);
        invalidatePermissions();
    }

    private boolean existsByCode(String code) {
        return permissionMapper.selectCount(
                new LambdaQueryWrapper<PermissionEntity>().eq(PermissionEntity::getCode, code)) > 0;
    }

    private void invalidatePermissions() {
        if (permissionCache != null) permissionCache.invalidateAll();
    }

    private void requireSystemAdministrator() {
        if (!TenantContext.isSuperAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "全局权限只能由系统管理员维护");
        }
    }
}
