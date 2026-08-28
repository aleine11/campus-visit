package com.campus.visit.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 修改密码请求参数（访客与管理员共用）
 *
 * 对标 architecture.md 1.3 修改密码：
 *   oldPassword  非空
 *   newPassword  6~20 位含字母与数字，且不能与原密码相同
 *
 * "不能与原密码相同"是跨字段比较，注解做不了（注解只校验单个字段）
 * 所以放在 Service 业务层里比较
 */
@Data
public class ChangePasswordDTO {

    /** 原密码明文 */
    @NotBlank(message = "原密码不能为空")
    private String oldPassword;

    /** 新密码明文（6~20 位且同时含字母与数字） */
    @NotBlank(message = "新密码不能为空")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d).{6,20}$", message = "新密码须为6~20位，且必须同时包含字母和数字")
    private String newPassword;
}
