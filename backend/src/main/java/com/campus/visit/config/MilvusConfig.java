package com.campus.visit.config;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Milvus 客户端配置
 *
 * 只负责建立与 Milvus（127.0.0.1:19530）的 gRPC 连接并注册为 Spring Bean；
 * 集合（campus_knowledge）的创建/索引/加载逻辑在 rag.MilvusService 的
 * ensureCollection() 中完成（幂等：已存在就跳过）。
 *
 * 为什么不在启动时强校验 Milvus？—— 传统业务（公告/预约）不依赖 Milvus，
 * 即使 Docker 没启动，后端其他模块也要能正常用（优雅降级）。
 */
@Slf4j
@Configuration
public class MilvusConfig {

    @Value("${campus.milvus.host}")
    private String host;

    @Value("${campus.milvus.port}")
    private int port;

    @Bean(destroyMethod = "close")
    public MilvusServiceClient milvusServiceClient() {
        MilvusServiceClient client = new MilvusServiceClient(
                ConnectParam.newBuilder().withHost(host).withPort(port).build());
        log.info("Milvus 客户端连接成功: {}:{}", host, port);
        return client;
    }
}
