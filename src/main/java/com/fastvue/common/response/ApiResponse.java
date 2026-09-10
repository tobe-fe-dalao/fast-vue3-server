package com.fastvue.common.response;

import com.fastvue.common.exception.ErrorCode;

/**
 * 统一响应信封。
 *
 * <p>约定：{@code code == 0} 表示成功，非 0 表示失败，具体语义见 {@link ErrorCode}。</p>
 *
 * @param <T> 数据载荷类型
 */
public record ApiResponse<T>(int code, String message, T data) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "success", data);
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>(0, "success", null);
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.code(), errorCode.message(), null);
    }

    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message) {
        return new ApiResponse<>(errorCode.code(), message, null);
    }
}
