package com.campus.visit.common;

import jakarta.servlet.ServletException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 *
 * 任何接口抛出异常都来这里，统一返回 Result 格式，前端拿到的永远是统一结构
 *
 * 处理三类异常：
 *  1. BusinessException：业务异常（如名额满、未登录等），用业务码返回
 *  2. 参数校验异常：@Valid 校验失败，提取所有字段错误拼成消息
 *  3. 其他未知异常：兜底返回 500
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常 */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常：code={}, msg={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /** @Valid 参数校验异常（POST Body） */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidException(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败：{}", msg);
        return Result.error(ResultCode.PARAM_INVALID.getCode(), msg);
    }

    /** 表单参数校验异常（GET Query） */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        return Result.error(ResultCode.PARAM_INVALID.getCode(), msg);
    }

    /** Servlet 异常 */
    @ExceptionHandler(ServletException.class)
    public Result<Void> handleServletException(ServletException e) {
        log.error("Servlet 异常", e);
        return Result.error(ResultCode.SERVER_ERROR);
    }

    /** 兜底：所有未知异常 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("未知异常", e);
        return Result.error(ResultCode.SERVER_ERROR);
    }
}
