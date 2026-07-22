package com.tripdesigner.common.exception;
/**
 * 全局异常处理器。
 * 使用 @RestControllerAdvice 统一处理各类异常：
 * - BizException: 业务异常，返回对应状态码
 * - MethodArgumentNotValidException: 参数校验失败
 * - AuthenticationException: 认证失败
 * - AccessDeniedException: 权限不足
 * - Exception: 其他未预期异常，返回 500
 */

import com.tripdesigner.common.response.Result;
import com.tripdesigner.common.response.ResultCode;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Void>> handleBiz(BizException ex) {
        log.warn("biz exception: code={}, msg={}", ex.getResultCode().getCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.OK).body(attachTrace(Result.fail(ex.getResultCode(), ex.getMessage())));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("validation failed: {}", msg);
        return ResponseEntity.badRequest().body(attachTrace(Result.fail(ResultCode.COMMON_BAD_REQUEST, msg)));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Result<Void>> handleAuth(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(attachTrace(Result.fail(ResultCode.AUTH_TOKEN_INVALID, ex.getMessage())));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(attachTrace(Result.fail(ResultCode.AUTH_TOKEN_INVALID, "access denied")));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleAny(Exception ex) {
        log.error("unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(attachTrace(Result.fail(ResultCode.COMMON_INTERNAL_ERROR)));
    }

    private Result<Void> attachTrace(Result<Void> r) {
        return r.withTraceId(MDC.get("traceId"));
    }
}
