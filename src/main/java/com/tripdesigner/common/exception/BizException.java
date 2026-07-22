package com.tripdesigner.common.exception;
/**
 * 业务异常基类。
 * 包含 ResultCode 枚举，用于统一返回错误码和错误消息。
 * 由 GlobalExceptionHandler 统一捕获并返回标准响应格式。
 */

import com.tripdesigner.common.response.ResultCode;
import lombok.Getter;

@Getter
public class BizException extends RuntimeException {
    private final ResultCode resultCode;

    public BizException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.resultCode = resultCode;
    }

    public BizException(ResultCode resultCode, String message) {
        super(message);
        this.resultCode = resultCode;
    }

    public BizException(ResultCode resultCode, String message, Throwable cause) {
        super(message, cause);
        this.resultCode = resultCode;
    }
}
