package com.campus.visit.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置 - 路径拦截器配置占位
 *
 * 实际的 JWT 拦截器将在 Stage2 用户认证模块开发时实现
 * 这里保留配置入口
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${campus.jwt.header}")
    private String header;

    /**
     * 注册拦截器（待 Stage2 用户模块实现 JwtInterceptor 后取消注释）
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // JwtInterceptor 在 Stage2 用户模块开发
        // registry.addInterceptor(jwtInterceptor)
        //         .addPathPatterns("/**")
        //         .excludePathPatterns("/auth/login", "/auth/register", "/notice/list", "/session/available");
    }
}
