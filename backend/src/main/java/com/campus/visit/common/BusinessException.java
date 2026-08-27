package com.campus.visit.common;

import lombok.Getter;

/**
 * 业务异常
 *
 * 用法：throw new BusinessException(ResultCode.SESSION_FULL);
 *
 * 全局异常处理器 GlobalExceptionHandler 会捕获这个异常并统一返回 Result
 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(ResultCode resultCode) {
        super(resultCode.getMessage());
        this.code = resultCode.getCode();
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
