package com.campus.visit.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 当前登录用户上下文 —— 用 ThreadLocal 在线程内传递
 *
 * 用法：
 *   JwtAuthInterceptor 解析完 token → UserContext.set(loginUser)
 *   Controller / Service 里 → UserContext.get() 直接拿
 *   请求结束后 → JwtAuthInterceptor.afterCompletion() 调 UserContext.clear()
 *
 * 为什么用 ThreadLocal？
 *   Spring MVC 一个请求对应一个线程，ThreadLocal 是线程隔离的
 *   不需要方法层层传参，在任何地方都能拿到当前用户
 *
 * 为什么请求结束必须 clear？
 *   Tomcat 线程池复用线程，如果不清理，上一个请求的用户信息
 *   会"泄漏"到下一个请求，严重的安全漏洞！
 */
public class UserContext {

    /** ThreadLocal 存 LoginUser */
    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    /** 设置当前用户 */
    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    /** 获取当前用户（可能为 null，说明未登录） */
    public static LoginUser get() {
        return HOLDER.get();
    }

    /** 清除（请求结束后必须调） */
    public static void clear() {
        HOLDER.remove();
    }

    /**
     * 便捷方法：获取当前用户ID
     * 如果未登录直接抛空指针，调用方要确保已登录（通常由拦截器保证）
     */
    public static Long getUserId() {
        LoginUser user = HOLDER.get();
        if (user == null) {
            throw new IllegalStateException("当前线程无登录用户，请检查 JwtAuthInterceptor 是否生效");
        }
        return user.getUserId();
    }

    /** 便捷方法：判断当前用户是否管理员 */
    public static boolean isAdmin() {
        LoginUser user = HOLDER.get();
        return user != null && "admin".equals(user.getRole());
    }

    /** 便捷方法：判断当前用户是否超管 */
    public static boolean isSuper() {
        LoginUser user = HOLDER.get();
        return user != null && Boolean.TRUE.equals(user.getIsSuper());
    }

    /**
     * 登录用户信息 —— 从 JWT payload 解析出来放这里
     * 字段与 JwtUtil.generateToken 的 claims 完全对应
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginUser {
        /** 用户ID */
        private Long userId;
        /** 角色：visitor / admin */
        private String role;
        /** 是否超管 */
        private Boolean isSuper;
        /** 用户名 */
        private String username;
    }
}
