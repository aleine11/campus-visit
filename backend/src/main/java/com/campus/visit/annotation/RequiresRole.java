package com.campus.visit.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记接口所需角色
 *
 * 用法：
 *   @RequiresRole("admin")           // 仅管理员可访问（同时隐含需要登录）
 *   @RequiresRole(value = "admin", superOnly = true)  // 仅超管
 *
 * 拦截器逻辑（在 @RequiresLogin 校验通过之后）：
 *   获取注解的 value → 比对 JWT payload 里的 role
 *   superOnly=true 时额外检查 isSuper=true
 *   不匹配 → 抛 403 FORBIDDEN
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresRole {

    /** 要求的角色名：visitor / admin */
    String value();

    /** 是否仅超管可访问（默认 false） */
    boolean superOnly() default false;
}
