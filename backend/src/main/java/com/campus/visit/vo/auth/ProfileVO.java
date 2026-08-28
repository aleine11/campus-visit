package com.campus.visit.vo.auth;

import lombok.Builder;
import lombok.Data;

/**
 * 当前登录人信息返回对象
 *
 * 对标 architecture.md 1.4 当前登录人信息：
 *   userId    用户 ID
 *   role      visitor / admin
 *   username  用户名
 *   realName  真实姓名
 *   phone     访客有，管理员为 null
 *   isSuper   管理员有，访客为 false
 *
 * 用 @Builder 构建器模式：字段多且部分为 null 时，比一长串构造器参数可读性好
 *   ProfileVO.builder().userId(1L).role("visitor")....build()
 */
@Data
@Builder
public class ProfileVO {

    /** 用户 ID */
    private Long userId;

    /** 角色：visitor / admin */
    private String role;

    /** 用户名 */
    private String username;

    /** 真实姓名 */
    private String realName;

    /** 手机号（访客有，管理员为 null） */
    private String phone;

    /** 是否超管（管理员有，访客为 false） */
    private Boolean isSuper;
}
