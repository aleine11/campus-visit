package com.campus.visit.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记接口需要登录
 *
 * 用法：加在 Controller 方法或类上
 *   @RequiresLogin                    // 只需登录，不限角色
 *   @RequiresRole("admin")            // 同时要求管理员角色（@RequiresRole 里已经包含了登录检查）
 *
 * 拦截器逻辑：
 *   无此注解 → 放行（公开接口）
 *   有此注解 → 检查 Authorization 头 → 解析 JWT → 写入 UserContext
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresLogin {
}
