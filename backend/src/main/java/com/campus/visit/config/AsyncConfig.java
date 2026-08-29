package com.campus.visit.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步任务配置（对标 architecture.md 模块 8.2：上传后异步解析，前端轮询 status）
 *
 * 知识库解析链路（解析→分块→向量化→写 Milvus）单文档可能耗时数十秒，
 * 必须放到独立线程池异步执行，HTTP 请求立即返回文档 ID。
 *
 * 线程池设计：核心 1 / 最大 1 / 队列 50 —— 故意串行！
 *   向量化是 CPU 大户，多文档并发解析会互相抢 CPU 导致全部变慢；
 *   排队串行处理对"千级文档"的毕设场景是最优解。
 */
@EnableAsync
@Configuration
public class AsyncConfig {

    @Bean("knowledgeExecutor")
    public Executor knowledgeExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("doc-parse-");
        executor.initialize();
        return executor;
    }
}
