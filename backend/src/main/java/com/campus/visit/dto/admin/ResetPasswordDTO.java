package com.campus.visit.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 重置管理员密码请求参数（对标 architecture.md 7.3）
 *
 * 规则与注册/新增管理员一致：6~20 位且同时含字母与数字
 */
@Data
public class ResetPasswordDTO {

    /** 新密码（明文，入库前 BCrypt 加密） */
    @NotBlank(message = "新密码不能为空")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d).{6,20}$", message = "密码须为6~20位，且必须同时包含字母和数字")
    private String newPassword;
}
