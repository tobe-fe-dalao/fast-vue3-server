package com.fastvue.common.exception;

import com.fastvue.common.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理。
 *
 * <p>统一将各类异常转换为 {@link ApiResponse}，避免在 Controller 中散落 try/catch。</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常。
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        log.warn("业务异常: {}", ex.getMessage());
        return build(ex.getErrorCode(), ex.getMessage());
    }

    /**
     * 登录失败（用户名或密码错误）。
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        log.warn("登录失败: 用户名或密码错误");
        return build(ErrorCode.INVALID_USERNAME_OR_PASSWORD);
    }

    /**
     * 唯一键或外键约束冲突。避免将用户可修正的输入误报为 500。
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(
            DataIntegrityViolationException ex) {
        log.warn("数据约束冲突 ({})", ex.getClass().getSimpleName());
        return build(ErrorCode.CONFLICT, "数据已存在，或关联的资源不存在");
    }

    /**
     * 请求体参数校验失败（@RequestBody + @Valid）。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex) {
        String message =
                ex.getBindingResult().getFieldErrors().stream()
                        .findFirst()
                        .map(f -> f.getField() + ": " + f.getDefaultMessage())
                        .orElse(ErrorCode.VALIDATION_ERROR.message());
        return build(ErrorCode.VALIDATION_ERROR, message);
    }

    /**
     * 表单/查询参数绑定校验失败（@ModelAttribute + @Valid）。
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBind(BindException ex) {
        String message =
                ex.getBindingResult().getFieldErrors().stream()
                        .findFirst()
                        .map(f -> f.getField() + ": " + f.getDefaultMessage())
                        .orElse(ErrorCode.VALIDATION_ERROR.message());
        return build(ErrorCode.VALIDATION_ERROR, message);
    }

    /**
     * 方法级参数约束（@RequestParam/@PathVariable 上的约束）。
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
            ConstraintViolationException ex) {
        return build(ErrorCode.VALIDATION_ERROR, ex.getMessage());
    }

    /**
     * 无权限（@PreAuthorize 拒绝）。
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("权限拒绝: {}", ex.getMessage());
        return build(ErrorCode.FORBIDDEN);
    }

    /**
     * 静态资源不存在（避免吞掉 404）。
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResource(NoResourceFoundException ex) {
        return build(ErrorCode.NOT_FOUND);
    }

    @ExceptionHandler({
        org.springframework.http.converter.HttpMessageNotReadableException.class,
        org.springframework.web.bind.ServletRequestBindingException.class,
        org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
        org.springframework.web.method.annotation.HandlerMethodValidationException.class,
        org.springframework.web.multipart.support.MissingServletRequestPartException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleInvalidRequest(Exception ex) {
        return build(ErrorCode.VALIDATION_ERROR);
    }

    @ExceptionHandler(org.springframework.dao.DataAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleDatabaseUnavailable(
            org.springframework.dao.DataAccessException ex) {
        log.error("Database operation failed ({})", ex.getClass().getSimpleName());
        return build(ErrorCode.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethod(Exception ex) {
        return ResponseEntity.status(405).body(ApiResponse.error(ErrorCode.METHOD_NOT_ALLOWED));
    }

    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMediaType(Exception ex) {
        return ResponseEntity.status(415).body(ApiResponse.error(ErrorCode.UNSUPPORTED_MEDIA_TYPE));
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleUploadSize(Exception ex) {
        return ResponseEntity.status(413).body(ApiResponse.error(ErrorCode.PAYLOAD_TOO_LARGE));
    }

    /**
     * 兜底系统异常。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        log.error("系统异常", ex);
        return build(ErrorCode.INTERNAL_ERROR);
    }

    private ResponseEntity<ApiResponse<Void>> build(ErrorCode errorCode) {
        return build(errorCode, errorCode.message());
    }

    private ResponseEntity<ApiResponse<Void>> build(ErrorCode errorCode, String message) {
        HttpStatus status =
                errorCode.httpStatus() != null
                        ? errorCode.httpStatus()
                        : HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(status).body(ApiResponse.error(errorCode, message));
    }
}
