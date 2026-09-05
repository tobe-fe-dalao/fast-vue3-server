package com.fastvue.module.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fastvue.common.BusinessException;
import com.fastvue.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 用户业务逻辑。
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final UserConverter userConverter;
    private final PasswordEncoder passwordEncoder;

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
        result.setRecords(page.getRecords().stream().map(entity -> {
            UserVO vo = userConverter.toVO(entity);
            return new UserVO(
                    vo.id(), vo.username(), vo.nickname(), vo.email(), vo.phone(),
                    vo.status(), userMapper.selectRoleCodes(entity.getId()),
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
     * 创建用户。
     */
    @Transactional
    public UserVO create(CreateUserRequest request) {
        if (existsByUsername(request.username())) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }

        UserEntity entity = new UserEntity();
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
            userMapper.deleteUserRoles(id);
            if (!request.roleIds().isEmpty()) {
                userMapper.insertUserRoles(id, request.roleIds());
            }
        }

        return getById(id);
    }

    /**
     * 删除用户（逻辑删除）。
     */
    @Transactional
    public void delete(Long id) {
        if (userMapper.selectById(id) == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        userMapper.deleteById(id);
        userMapper.deleteUserRoles(id);
    }

    private boolean existsByUsername(String username) {
        return userMapper.selectCount(
                new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getUsername, username)) > 0;
    }
}
