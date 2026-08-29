package com.campus.visit.rag;

import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.campus.visit.common.BusinessException;
import com.campus.visit.common.ResultCode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 阿里百炼 DashScope 客户端封装（对标 architecture.md 模块 8.2：LLM Client）
 *
 * SDK 2.20.6 同步调用：Generation.call(GenerationParam)（半双工一次性返回）
 * apiKey 从配置注入后写进 Generation 实例（构造器带 protocol 参数的那条路太重，
 * 这里用 DashScope 官方推荐的全局 apiKey 设置方式，线程安全）
 * 系统提示词 + 用户提示词由 ChatService 组装后传入，本类只管"发请求 → 拿文本/用量"
 *
 * 为什么封装一层？——可替换性（设计文档非功能 4.4）：
 * 将来换模型（qwen-max / 别家 API）只改这个类，业务代码零改动
 */
@Slf4j
@Component
public class DashScopeClient {

    @Resource
    private Generation generation;

    @Value("${campus.dashscope.api-key}")
    private String apiKey;

    @Value("${campus.dashscope.model}")
    private String model;

    /**
     * 同步问答
     *
     * @param systemPrompt 系统提示词（RAG 规则约束）
     * @param userPrompt   用户提示词（知识库片段 + 问题）
     * @return answer + tokens（总消耗）
     */
    public LlmResult chat(String systemPrompt, String userPrompt) {
        Message systemMsg = Message.builder()
                .role(Role.SYSTEM.getValue())
                .content(systemPrompt)
                .build();
        Message userMsg = Message.builder()
                .role(Role.USER.getValue())
                .content(userPrompt)
                .build();

        GenerationParam param = GenerationParam.builder()
                .apiKey(apiKey)
                .model(model)
                .messages(List.of(systemMsg, userMsg))
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .temperature(0.7f)
                .maxTokens(1024)
                .build();

        try {
            GenerationResult result = generation.call(param);
            String answer = result.getOutput().getChoices().get(0).getMessage().getContent();
            Integer totalTokens = result.getUsage() == null ? null : result.getUsage().getTotalTokens();
            log.info("DashScope 调用成功: model={}, tokens={}, answerLen={}", model, totalTokens, answer.length());
            return new LlmResult(answer, totalTokens);
        } catch (ApiException e) {
            log.error("DashScope API 异常: {}", e.getMessage());
            throw new BusinessException(ResultCode.AI_SERVICE_ERROR, "AI 服务调用失败: " + e.getMessage());
        } catch (NoApiKeyException | InputRequiredException e) {
            log.error("DashScope 参数异常: {}", e.getMessage());
            throw new BusinessException(ResultCode.AI_SERVICE_ERROR, "AI 服务配置错误: " + e.getMessage());
        }
    }

    /** 调用结果（answer + token 用量） */
    public record LlmResult(String answer, Integer totalTokens) {
    }
}
