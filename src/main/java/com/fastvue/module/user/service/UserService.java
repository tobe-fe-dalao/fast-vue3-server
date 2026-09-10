package com.fastvue.module.user.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fastvue.common.exception.BusinessException;
import com.fastvue.common.exception.ErrorCode;
import com.fastvue.module.user.api.CreateUserRequest;
import com.fastvue.module.user.api.UpdateUserRequest;
import com.fastvue.module.user.api.UserQueryRequest;
import com.fastvue.module.user.api.UserVO;
import com.fastvue.module.user.persistence.UserEntity;
import com.fastvue.module.user.persistence.UserMapper;
import com.fastvue.module.role.persistence.RoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.fastvue.security.PermissionCache;
import com.fastvue.infrastructure.tenant.TenantContext;
import com.fastvue.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 用户业务逻辑。
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final UserConverter userConverter;
    private final PasswordEncoder passwordEncoder;
    private final RoleMapper roleMapper;

    @Autowired(required = false)
    private PermissionCache permissionCache;

    /**
     * 分页查询用户。
     */
    public Page<UserVO> page(UserQueryRequest query) {
        LambdaQueryWrapper<UserEntity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.keyword())) {
            wrapper.and(w -> w.like(UserEntity::getUsername, query.keyword())
                    .or()
                    .like(UserEntity::getNickname, query.keyword())
                    .or()
                    .like(UserEntity::getEmail, query.keyword()));
        }
        if (StringUtils.hasText(query.status())) {
            wrapper.eq(UserEntity::getStatus, query.status());
        }
        wrapper.orderByAsc(UserEntity::getId);

        Page<UserEntity> page = userMapper.selectPage(
                new Page<>(query.page(), query.pageSize()), wrapper);

        Page<UserVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<Long> userIds = page.getRecords().stream().map(UserEntity::getId).toList();
        Map<Long, List<String>> rolesByUser = userIds.isEmpty() ? Map.of()
                : userMapper.selectRoleRows(userIds).stream().collect(Collectors.groupingBy(
                        UserMapper.UserRoleRow::userId,
                        Collectors.mapping(UserMapper.UserRoleRow::code, Collectors.toList())));
        result.setRecords(page.getRecords().stream().map(entity -> {
            UserVO vo = userConverter.toVO(entity);
            return new UserVO(
                    vo.id(), vo.username(), vo.nickname(), vo.email(), vo.phone(),
                    vo.status(), rolesByUser.getOrDefault(entity.getId(), List.of()),
                    vo.createdAt(), vo.updatedAt());
        }).toList());
        return result;
    }

    /**
     * 查询单个用户。
     */
    public UserVO getById(Long id) {
        UserEntity entity = userMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        UserVO vo = userConverter.toVO(entity);
        return new UserVO(
                vo.id(), vo.username(), vo.nickname(), vo.email(), vo.phone(),
                vo.status(), userMapper.selectRoleCodes(id),
                vo.createdAt(), vo.updatedAt());
    }

    /**
     * 查询用户通过角色获得的菜单 id。
     */
    public List<Long> listMenuIds(Long userId) {
        return userMapper.selectMenuIds(userId);
    }

    /**
     * 创建用户。
     */
    @Transactional
    public UserVO create(CreateUserRequest request) {
        if (existsByUsername(request.username())) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }

        UserEntity entity = new UserEntity();
        entity.setTenantId(TenantContext.tenantId());
        entity.setUsername(request.username());
        entity.setPassword(passwordEncoder.encode(request.password()));
        entity.setNickname(request.nickname());
        entity.setEmail(request.email());
        entity.setPhone(request.phone());
        entity.setStatus("active");
        userMapper.insert(entity);

        return getById(entity.getId());
    }

    /**
     * 更新用户。
     */
    @Transactional
    public UserVO update(Long id, UpdateUserRequest request) {
        requireSystemAdministratorOwner(id);
        UserEntity entity = userMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        if (request.password() != null && !request.password().isBlank()) {
            entity.setPassword(passwordEncoder.encode(request.password()));
        }
        if (request.nickname() != null) {
            entity.setNickname(request.nickname());
        }
        if (request.email() != null) {
            entity.setEmail(request.email());
        }
        if (request.phone() != null) {
            entity.setPhone(request.phone());
        }
        if (StringUtils.hasText(request.status())) {
            entity.setStatus(request.status());
        }
        userMapper.updateById(entity);

        // 重新绑定角色
        if (request.roleIds() != null) {
            if (!TenantContext.isSuperAdmin()
                    && (SecurityUtils.currentUserOrNull() == null
                    || !SecurityUtils.currentUserOrNull().roles().contains("tenant-admin"))) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "角色分配需要租户管理员权限");
            }
            List<Long> uniqueRoleIds = request.roleIds().stream().distinct().toList();
            if (uniqueRoleIds.contains(1L) && !TenantContext.isSuperAdmin()) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "系统管理员角色不能授予其他用户");
            }
            long existingRoles = uniqueRoleIds.isEmpty() ? 0 : TenantContext.runAs(entity.getTenantId(), false,
                    () -> roleMapper.selectBatchIds(uniqueRoleIds).size());
            if (existingRoles != uniqueRoleIds.size()) {
                throw new BusinessException(ErrorCode.ROLE_NOT_FOUND, "角色不存在或不属于当前租户");
            }
            userMapper.deleteUserRoles(id);
            if (!uniqueRoleIds.isEmpty()) {
                userMapper.insertUserRoles(entity.getTenantId(), id, uniqueRoleIds);
            }
        }
        invalidatePermissions();

        return getById(id);
    }

    /**
     * 删除用户（逻辑删除）。
     */
    @Transactional
    public void delete(Long id) {
        requireSystemAdministratorOwner(id);
        if (userMapper.selectById(id) == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        userMapper.deleteById(id);
        userMapper.deleteUserRoles(id);
        invalidatePermissions();
    }

    private boolean existsByUsername(String username) {
        return userMapper.selectCount(
                new LambdaQueryWrapper<UserEntity>()
                        .eq(UserEntity::getTenantId, TenantContext.tenantId())
                        .eq(UserEntity::getUsername, username)) > 0;
    }

    private void invalidatePermissions() {
        if (permissionCache != null) permissionCache.invalidateAll();
    }

    private void requireSystemAdministratorOwner(Long userId) {
        if (Long.valueOf(1L).equals(userId) && !TenantContext.isSuperAdmin()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "系统管理员账号只能由系统管理员修改");
        }
    }
}
