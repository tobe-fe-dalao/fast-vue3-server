package com.fastvue.module.permission;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fastvue.common.BusinessException;
import com.fastvue.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 权限业务逻辑。
 */
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionMapper permissionMapper;
    private final PermissionConverter permissionConverter;

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
        if (existsByCode(request.code())) {
            throw new BusinessException(ErrorCode.CONFLICT, "权限编码已存在");
        }
        PermissionEntity entity = new PermissionEntity();
        entity.setCode(request.code());
        entity.setName(request.name());
        entity.setDescription(request.description());
        permissionMapper.insert(entity);
        return permissionConverter.toVO(entity);
    }

    /**
     * 更新权限。
     */
    public PermissionVO update(Long id, PermissionRequest request) {
        PermissionEntity entity = permissionMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.PERMISSION_NOT_FOUND);
        }
        entity.setName(request.name());
        entity.setDescription(request.description());
        permissionMapper.updateById(entity);
        return permissionConverter.toVO(entity);
    }

    /**
     * 删除权限。
     */
    public void delete(Long id) {
        if (permissionMapper.selectById(id) == null) {
            throw new BusinessException(ErrorCode.PERMISSION_NOT_FOUND);
        }
        permissionMapper.deleteById(id);
    }

    private boolean existsByCode(String code) {
        return permissionMapper.selectCount(
                new LambdaQueryWrapper<PermissionEntity>().eq(PermissionEntity::getCode, code)) > 0;
    }
}
