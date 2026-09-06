package com.fastvue.common;

import org.springframework.http.HttpStatus;

/**
 * 统一错误码定义。
 *
 * <p>code 同时承载两层语义：业务错误码（响应体中的 {@code code}）与对应的 HTTP 状态码，
 * 便于全局异常处理直接映射。</p>
 */
public enum ErrorCode {

    // ---- 通用 ----
    VALIDATION_ERROR(400, "参数校验失败"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "数据冲突"),
    INTERNAL_ERROR(500, "系统异常"),

    // ---- 认证 ----
    INVALID_USERNAME_OR_PASSWORD(401, "用户名或密码错误"),
    INVALID_REFRESH_TOKEN(401, "刷新令牌无效或已过期"),

    // ---- 用户 ----
    USER_NOT_FOUND(404, "用户不存在"),
    USER_ALREADY_EXISTS(409, "用户名已存在"),
    USERNAME_REQUIRED(400, "用户名不能为空"),

    // ---- 角色 ----
    ROLE_NOT_FOUND(404, "角色不存在"),
    ROLE_ALREADY_EXISTS(409, "角色编码已存在"),

    // ---- 权限 ----
    PERMISSION_NOT_FOUND(404, "权限不存在"),
    PERMISSION_DENIED(403, "权限不足"),

    // ---- 菜单 ----
    MENU_NOT_FOUND(404, "菜单不存在"),

    // ---- 内容管理 ----
    ARTICLE_NOT_FOUND(404, "文章不存在"),
    CATEGORY_NOT_FOUND(404, "分类不存在"),
    CATEGORY_ALREADY_EXISTS(409, "分类名称或标识已存在");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int code() {
        return code;
    }

    public String message() {
        return message;
    }

    public HttpStatus httpStatus() {
        return HttpStatus.resolve(code);
    }
}
