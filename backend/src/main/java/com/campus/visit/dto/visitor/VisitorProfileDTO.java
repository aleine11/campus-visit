package com.campus.visit.dto.visitor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 访客修改个人信息请求参数（对标 architecture.md 6.4 VisitorProfileDTO）
 *
 * 只允许改 realName 和 phone 两个字段：
 *   username 不给改（登录身份锚点）；
 *   password 不在这改（走模块 1 的 /auth/password 修改密码接口）
 *
 * 该接口由 VisitorProfileController 挂 @RequiresLogin 提供给访客本人
 */
@Data
public class VisitorProfileDTO {

    /** 真实姓名（2~10 字） */
    @NotBlank(message = "真实姓名不能为空")
    @Size(min = 2, max = 10, message = "真实姓名长度须在2~10字之间")
    private String realName;

    /** 手机号（11 位大陆手机号） */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
}
