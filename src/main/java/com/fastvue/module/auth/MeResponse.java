package com.fastvue.module.auth;

import java.util.List;

/**
 * 当前登录用户信息。
 *
 * @param id          用户 id
 * @param username    用户名
 * @param nickname    昵称
 * @param roles       角色编码列表
 * @param permissions 权限字符串列表
 */
public record MeResponse(Long id, String username, String nickname, List<String> roles, List<String> permissions) {
}
