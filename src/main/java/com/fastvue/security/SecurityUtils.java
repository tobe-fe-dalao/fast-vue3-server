package com.fastvue.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * 安全上下文工具。
 *
 * <p>提供获取当前登录用户 id / 用户名的便捷方法，供审计填充与业务逻辑使用。</p>
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * 获取当前登录用户 id；未登录时返回 null。
     */
    public static Long currentUserIdOrNull() {
        Object principal = currentPrincipal();
        if (principal instanceof LoginUser loginUser) {
            return loginUser.id();
        }
        return null;
    }

    /**
     * 获取当前登录用户名；未登录时返回 null。
     */
    public static String currentUsernameOrNull() {
        Object principal = currentPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        return null;
    }

    /**
     * 获取当前登录用户；未登录时抛出 {@link IllegalArgumentException}。
     */
    public static LoginUser currentUser() {
        LoginUser loginUser = currentUserOrNull();
        if (loginUser != null) return loginUser;
        throw new IllegalStateException("当前未登录");
    }

    public static LoginUser currentUserOrNull() {
        Object principal = currentPrincipal();
        return principal instanceof LoginUser loginUser ? loginUser : null;
    }

    public static Long currentTenantId() {
        LoginUser user = currentUser();
        return user.tenantId();
    }

    private static Object currentPrincipal() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return authentication.getPrincipal();
    }
}
