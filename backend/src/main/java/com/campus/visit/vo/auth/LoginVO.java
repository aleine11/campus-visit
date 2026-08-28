package com.campus.visit.vo.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录成功返回对象
 *
 * 对标 architecture.md 1.2 统一登录响应：
 *   token     JWT 令牌（前端存起来，每次请求放 Authorization 头）
 *   role      visitor / admin（前端据此跳转不同首页）
 *   userId    用户 ID
 *   realName  真实姓名（前端显示欢迎语）
 *   isSuper   是否超管（仅 role=admin 时有意义；访客恒为 false）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO {

    /** JWT 令牌 */
    private String token;

    /** 角色：visitor / admin */
    private String role;

    /** 用户 ID */
    private Long userId;

    /** 真实姓名 */
    private String realName;

    /** 是否超管（仅 admin 有意义） */
    private Boolean isSuper;
}
