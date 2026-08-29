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

    /** 带自定义消息的构造（错误码取自枚举，消息可附加细节，如"文档解析失败: xxx"） */
    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
