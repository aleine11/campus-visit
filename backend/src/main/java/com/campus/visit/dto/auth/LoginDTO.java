package com.campus.visit.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 统一登录请求参数（访客与管理员共用）
 *
 * 对标 architecture.md 1.2 统一登录：
 *   username  非空（访客或管理员账号）
 *   password  非空
 *
 * 注意：这里只做"非空"校验，不做格式校验
 * 因为登录是"管理员账号"和"访客账号"混在一个口进来，
 * 两边规则不同，格式校验放在业务层判断即可（错了统一报 40010）
 */
@Data
public class LoginDTO {

    /** 用户名 */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 密码明文 */
    @NotBlank(message = "密码不能为空")
    private String password;
}
