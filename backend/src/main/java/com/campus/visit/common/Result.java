package com.campus.visit.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一返回格式 Result<T>
 *
 * 所有接口统一返回这个结构，前端拿到后先看 code：
 * - 200 = 成功，直接拿 data
 * - 非 200 = 失败，看 message
 *
 * 字段名对齐 architecture.md 第五章：code / message / data / timestamp
 *
 * 示例：
 * 成功：Result.success(用户对象)
 * 失败：Result.fail(ResultCode.USERNAME_OR_PASSWORD_ERROR)
 */
@Data
public class Result<T> implements Serializable {

    /** 状态码：200 成功，其他失败 */
    private Integer code;

    /** 提示消息（与 architecture.md 的 message 字段对齐） */
    private String message;

    /** 数据载荷 */
    private T data;

    /** 时间戳，方便排查问题 */
    private Long timestamp;

    public Result() {
        this.timestamp = System.currentTimeMillis();
    }

    /** 成功返回，无数据 */
    public static <T> Result<T> success() {
        Result<T> r = new Result<>();
        r.setCode(ResultCode.SUCCESS.getCode());
        r.setMessage(ResultCode.SUCCESS.getMessage());
        return r;
    }

    /** 成功返回，带数据 */
    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.setCode(ResultCode.SUCCESS.getCode());
        r.setMessage(ResultCode.SUCCESS.getMessage());
        r.setData(data);
        return r;
    }

    /** 失败返回，自定义状态码和消息 */
    public static <T> Result<T> fail(Integer code, String message) {
        Result<T> r = new Result<>();
        r.setCode(code);
        r.setMessage(message);
        return r;
    }

    /** 失败返回，用枚举状态码 */
    public static <T> Result<T> fail(ResultCode resultCode) {
        Result<T> r = new Result<>();
        r.setCode(resultCode.getCode());
        r.setMessage(resultCode.getMessage());
        return r;
    }
}
