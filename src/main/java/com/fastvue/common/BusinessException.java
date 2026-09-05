package com.fastvue.common;

import lombok.Getter;

/**
 * 业务异常，携带统一错误码。
 *
 * <p>Service 层抛出，由全局异常处理 {@code GlobalExceptionHandler} 统一转换为
 * {@link ApiResponse}，Controller 无需 try/catch。</p>
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.message());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
