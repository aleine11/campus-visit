package com.campus.visit.rag;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import com.campus.visit.common.BusinessException;
import com.campus.visit.common.ResultCode;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * BGE 中文嵌入模型服务（本地 ONNX 推理）
 *
 * 模型：bge-base-zh-v1.5（768 维，与设计文档/Milvus 集合一致；
 *       设计文档笔误写 small 版——small 实际 512 维，故用 base 版）
 *
 * 工作原理（三步）：
 *   1. HuggingFaceTokenizer 把文本切成 token 序列（input_ids + attention_mask）
 *   2. ONNX Runtime 跑 model.onnx，输出每个 token 的隐藏向量 [seq, 768]
 *   3. CLS 池化（取第一个 token 的向量，BGE 官方 pooling 方式）+ L2 归一化
 *
 * 模型缺失时的降级策略：允许应用正常启动（传统业务不依赖它），
 * 调用 embed 时抛 40040 并提示下载教程——避免"模型没下就把整个后端搞挂"。
 */
@Slf4j
@Component
public class EmbeddingService {

    private final OrtEnvironment ortEnv;
    private final OrtSession ortSession;
    private final HuggingFaceTokenizer tokenizer;

    public EmbeddingService(@Value("${campus.bge.model-path}") String modelPath) {
        Path dir = Paths.get(modelPath);
        Path onnx = dir.resolve("model.onnx");
        Path tokenizerJson = dir.resolve("tokenizer.json");

        if (!Files.exists(onnx) || !Files.exists(tokenizerJson)) {
            // 降级：不抛异常，允许应用启动；真正调用向量接口时再报错
            this.ortEnv = null;
            this.ortSession = null;
            this.tokenizer = null;
            log.warn("BGE 模型文件不完整（需要 model.onnx + tokenizer.json 位于 {}）。"
                    + "知识库解析功能不可用，请按知识点手册教程下载模型", dir.toAbsolutePath());
            return;
        }

        try {
            this.tokenizer = HuggingFaceTokenizer.newInstance(tokenizerJson,
                    Map.of("maxLength", "512", "truncation", "true"));
            this.ortEnv = OrtEnvironment.getEnvironment();
            OrtSession.SessionOptions options = new OrtSession.SessionOptions();
            this.ortSession = ortEnv.createSession(onnx.toString(), options);
            // 启动自检：跑一条真实文本验证维度
            float[] check = embedOnce("模型启动自检");
            log.info("BGE 嵌入模型加载成功: path={}, 输出维度={}", dir.toAbsolutePath(), check.length);
        } catch (Exception e) {
            throw new IllegalStateException("BGE 模型初始化失败: " + e.getMessage(), e);
        }
    }

    /** 是否可用（模型已加载） */
    public boolean isAvailable() {
        return ortSession != null;
    }

    /** 单文本向量化 */
    public float[] embed(String text) {
        if (!isAvailable()) {
            throw new BusinessException(ResultCode.AI_SERVICE_ERROR, "BGE 模型未加载，请先下载模型文件（教程见知识点手册）");
        }
        return embedOnce(text);
    }

    /** 批量向量化（解析流水线用） */
    public List<float[]> embedAll(List<String> texts) {
        List<float[]> result = new ArrayList<>(texts.size());
        for (String text : texts) {
            result.add(embedOnce(text));
        }
        return result;
    }

    /* ==================== 私有方法 ==================== */

    /** 真实推理：分词 → ONNX 前向 → CLS 池化 → L2 归一化 */
    private float[] embedOnce(String text) {
        try {
            // 1. 分词
            Encoding encoding = tokenizer.encode(text);
            long[] inputIds = encoding.getIds();
            long[] attentionMask = encoding.getAttentionMask();
            long[] tokenTypeIds = encoding.getTypeIds();
            if (tokenTypeIds == null || tokenTypeIds.length != inputIds.length) {
                tokenTypeIds = new long[inputIds.length];   // 兜底：全 0
            }

            // 2. 构造输入张量 [1, seqLen]
            Map<String, OnnxTensor> inputs = new HashMap<>();
            inputs.put("input_ids", OnnxTensor.createTensor(ortEnv, new long[][]{inputIds}));
            inputs.put("attention_mask", OnnxTensor.createTensor(ortEnv, new long[][]{attentionMask}));
            if (ortSession.getInputNames().contains("token_type_ids")) {
                inputs.put("token_type_ids", OnnxTensor.createTensor(ortEnv, new long[][]{tokenTypeIds}));
            }

            // 3. 前向推理 → [1, seqLen, 768]
            try (OrtSession.Result result = ortSession.run(inputs)) {
                float[][][] hidden = (float[][][]) result.get(0).getValue();
                return clsPool(hidden[0]);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("向量化失败: {}", e.getMessage());
            throw new BusinessException(ResultCode.AI_SERVICE_ERROR, "文本向量化失败: " + e.getMessage());
        }
    }

    /** CLS 池化（BGE 官方方式）：取第 0 个 token 的向量 + L2 归一化 */
    private float[] clsPool(float[][] tokenVectors) {
        float[] vector = tokenVectors[0].clone();
        double norm = 0.0;
        for (float v : vector) {
            norm += (double) v * v;
        }
        norm = Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] = (float) (vector[i] / norm);
            }
        }
        return vector;
    }

    /** 释放资源 */
    @PreDestroy
    public void destroy() {
        if (ortSession != null) {
            ortSession.close();
        }
        if (tokenizer != null) {
            tokenizer.close();
        }
    }
}
