package com.campus.visit.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 跨域配置 CorsConfig
 *
 * 前端跑在 5173 端口，后端跑在 8088 端口，浏览器默认会拦截跨域请求
 * 这里配置允许前端域名访问后端
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")  // 拦截所有路径
                .allowedOriginPatterns("*")  // 允许所有来源（生产建议改成具体域名）
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")  // 允许所有请求头
                .allowCredentials(true)  // 允许携带凭证
                .maxAge(3600);  // 预检请求缓存 1 小时
    }
}
