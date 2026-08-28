package com.campus.visit.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 访客注册请求参数
 *
 * 对标 architecture.md 1.1 访客注册：
 *   username  4~20 位字母数字下划线，唯一
 *   password  6~20 位且同时含字母与数字
 *   realName  2~10 字
 *   phone     11 位中国手机号
 *
 * 校验注解说明：
 *   @NotBlank  不能为 null / 空串 / 纯空格（String 专用）
 *   @Pattern   正则表达式校验（regex 参数）
 *   @Size      长度/个数范围校验（对 String 是字符长度）
 *   触发校验失败 → 抛 MethodArgumentNotValidException
 *   → GlobalExceptionHandler 统一转成 40001 参数校验失败
 */
@Data
public class RegisterDTO {

    /** 用户名（4~20 位字母数字下划线） */
    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{4,20}$", message = "用户名须为4~20位字母、数字或下划线")
    private String username;

    /** 密码明文（6~20 位且同时含字母与数字） */
    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d).{6,20}$", message = "密码须为6~20位，且必须同时包含字母和数字")
    private String password;

    /** 真实姓名（2~10 字） */
    @NotBlank(message = "真实姓名不能为空")
    @Size(min = 2, max = 10, message = "真实姓名须为2~10个字")
    private String realName;

    /** 联系手机号（11 位中国手机号，1 开头第二位 3-9） */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
}
