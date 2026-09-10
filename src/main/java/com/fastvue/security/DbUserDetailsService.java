package com.fastvue.security;

import com.fastvue.module.role.persistence.RoleMapper;
import com.fastvue.module.user.persistence.UserEntity;
import com.fastvue.module.user.persistence.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 从数据库加载用户认证信息。
 *
 * <p>将用户角色（以 {@code ROLE_*} 前缀）与权限字符串（如 {@code user:list}）
 * 映射为 Spring Security 的授权集合，供 {@code hasAuthority} / {@code hasRole} 判断。</p>
 */
@Service
@RequiredArgsConstructor
public class DbUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PermissionCache permissionCache;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity user = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserEntity>()
                        .eq(UserEntity::getUsername, username));
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        PermissionCache.Snapshot snapshot = permissionCache.get(user.getId(), () ->
                new PermissionCache.Snapshot(userMapper.selectRoleCodes(user.getId()),
                        userMapper.selectPermissionCodes(user.getId())));
        List<String> roleCodes = snapshot.roles();
        List<String> permissionCodes = snapshot.permissions();

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        roleCodes.forEach(code -> authorities.add(new SimpleGrantedAuthority("ROLE_" + code)));
        permissionCodes.forEach(code -> authorities.add(new SimpleGrantedAuthority(code)));

        return new LoginUser(
                user.getId(),
                user.getTenantId(),
                user.getUsername(),
                user.getPassword(),
                "active".equals(user.getStatus()),
                roleCodes,
                permissionCodes,
                authorities);
    }
}
