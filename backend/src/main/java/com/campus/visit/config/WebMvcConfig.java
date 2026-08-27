package com.campus.visit.config;

import com.campus.visit.interceptor.JwtAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置 —— 注册 JWT 拦截器
 *
 * 拦截范围与排除路径严格对标 architecture.md 第八章 8.3 节公开接口清单：
 * 公开接口（不需要登录）：
 * POST /api/auth/login
 * POST /api/auth/register
 * GET /api/notice/list、/api/notice/latest、/api/notice/{id}
 * GET /api/session/available、/api/session/latest、/api/session/{id}
 * GET /api/public/home
 *
 * 其他所有接口默认需要登录（Controller 方法加 @RequiresLogin / @RequiresRole 注解生效）
 * 拦截器通过注解判断是否需要鉴权，而非硬编码路径
 * 这里只需要注册拦截器到 /**，排除掉那些永远不需要鉴权的公开路径即可
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtAuthInterceptor jwtAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthInterceptor)
                .addPathPatterns("/**")
                // 公开接口 —— 永远不需要鉴权（直接放行）
                .excludePathPatterns(
                        "/auth/login",
                        "/auth/register",
                        "/notice/list",
                        "/notice/latest",
                        "/notice/**", // 注意：notice 前台都是公开的，管理端在 /admin/notice/**
                        "/session/available",
                        "/session/latest",
                        "/session/**", // 前台场次查询公开，管理端在 /admin/session/**
                        "/public/home",
                        // 静态资源和 Swagger
                        "/error",
                        "/doc.html",
                        "/webjars/**",
                        "/v3/api-docs/**");
    }
}
