package com.tripdesigner.common.response;
/**
 * 统一 API 响应封装。
 * 所有 REST API 都返回此格式，确保前端统一处理。
 * 包含：状态码(code)、消息(message)、数据(data)、追踪ID(traceId)
 * data 字段仅在有数据时序列化（@JsonInclude NON_NULL）。
 */

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> {
    private final int code;
    private final String message;
    private final T data;
    private final String traceId;

    private Result(int code, String message, T data, String traceId) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.traceId = traceId;
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data, null);
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> fail(ResultCode code) {
        return new Result<>(code.getCode(), code.getMessage(), null, null);
    }

    public static <T> Result<T> fail(ResultCode code, String message) {
        return new Result<>(code.getCode(), message, null, null);
    }

    public Result<T> withTraceId(String traceId) {
        return new Result<>(this.code, this.message, this.data, traceId);
    }
}
