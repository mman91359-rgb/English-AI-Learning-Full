package com.english.ai.common;

import lombok.Data;

import java.util.UUID;

/**
 * 统一响应结构
 * <pre>
 * {
 *   "code": 200,
 *   "message": "success",
 *   "data": {},
 *   "traceId": "xxx"
 * }
 * </pre>
 */
@Data
public class Result<T> {

    /** 200成功, 400参数错误, 401未授权, 500系统异常 */
    private int code;

    /** 提示信息 */
    private String message;

    /** 业务数据 */
    private T data;

    /** 全链路追踪ID */
    private String traceId;

    private Result() {
        this.traceId = UUID.randomUUID().toString().substring(0, 8);
    }

    // ========== 静态工厂方法 ==========

    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.message = "success";
        r.data = data;
        return r;
    }

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> error(int code, String message) {
        Result<T> r = new Result<>();
        r.code = code;
        r.message = message;
        return r;
    }

    public static <T> Result<T> error(String message) {
        return error(500, message);
    }

    public static <T> Result<T> badRequest(String message) {
        return error(400, message);
    }

    public static <T> Result<T> unauthorized(String message) {
        return error(401, message);
    }

    public static <T> Result<T> conflict(String message) {
        return error(409, message);
    }

}
