package com.campus.visit.config;

import com.alibaba.dashscope.aigc.generation.Generation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里百炼 DashScope 客户端配置
 *
 * Generation 实例本身无状态（apiKey 在每次请求的 GenerationParam 上携带，
 * 见 DashScopeClient），因此单例即可全局复用
 */
@Configuration
public class DashScopeConfig {

    @Bean
    public Generation generation() {
        return new Generation();
    }
}
