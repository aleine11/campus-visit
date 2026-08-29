package com.campus.visit.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新增管理员请求参数（对标 architecture.md 7.2 AdminSaveDTO）
 *
 * 校验规则与模块 1 RegisterDTO 完全一致（同一个系统的账号规范必须统一）：
 *   username  4~20 位字母数字下划线（唯一性在 Service 查库校验）
 *   password  6~20 位且同时含字母与数字
 *   realName  2~10 字
 *
 * 注意：新增管理员不提供 isSuper 参数——超管身份只能通过数据库手动设置，
 *       防止超管在页面上"制造超管"导致权限失控（安全设计）
 */
@Data
public class AdminSaveDTO {

    /** 管理员账号 */
    @NotBlank(message = "账号不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{4,20}$", message = "账号须为4~20位字母、数字或下划线")
    private String username;

    /** 初始密码（明文入库前 BCrypt 加密） */
    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d).{6,20}$", message = "密码须为6~20位，且必须同时包含字母和数字")
    private String password;

    /** 管理员姓名 */
    @NotBlank(message = "姓名不能为空")
    @Size(min = 2, max = 10, message = "姓名长度须在2~10字之间")
    private String realName;
}
