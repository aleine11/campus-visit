package com.campus.visit.rag;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.visit.entity.KnowledgeDoc;
import com.campus.visit.mapper.KnowledgeDocMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 文档解析流水线（异步，对标 architecture.md 8.2 业务逻辑）
 *
 * 五步链路：解析 → 分块 → 向量化 → 写 Milvus → 更新 MySQL 状态
 * 全链路任何一步失败 → knowledge_doc.status=2 + error_msg（可重新解析）
 *
 * 为什么单独一个类？——@Async 自调用失效坑：
 *   同一个 Bean 内部 this.method() 调用不经过 Spring 代理，异步会变同步。
 *   拆到独立 Bean，由 KnowledgeDocServiceImpl 跨 Bean 调用，代理才生效。
 */
@Slf4j
@Component
public class DocumentPipeline {

    /** 失败原因最长保存字符数（防 error_msg 超长） */
    private static final int ERROR_MSG_MAX = 200;

    @Resource
    private EmbeddingService embeddingService;
    @Resource
    private MilvusService milvusService;
    @Resource
    private KnowledgeDocMapper knowledgeDocMapper;

    @Value("${campus.rag.chunk-size}")
    private int chunkSize;

    @Value("${campus.rag.chunk-overlap}")
    private int chunkOverlap;

    /**
     * 异步解析入口（调用方立即返回，本方法在 doc-parse- 线程池排队执行）
     */
    @Async("knowledgeExecutor")
    public void processAsync(Long docId, String filePath, String fileName) {
        long start = System.currentTimeMillis();
        try {
            // 1. 解析文档 → 纯文本
            String text = DocParseUtil.parseFile(filePath, fileName);

            // 2. 分块（500 字 / 100 重叠）
            List<String> chunks = TextChunker.chunk(text, chunkSize, chunkOverlap);
            if (chunks.isEmpty()) {
                throw new IllegalStateException("分块结果为空");
            }

            // 3. 向量化（BGE 本地推理，CPU 数秒）
            List<float[]> vectors = embeddingService.embedAll(chunks);

            // 4. 写入 Milvus（失败抛 40050）
            milvusService.insertChunks(docId, fileName, chunks, vectors);

            // 5. 更新 MySQL：已完成（"先 Milvus 后 MySQL"顺序，database.md 6.4）
            KnowledgeDoc doc = new KnowledgeDoc();
            doc.setId(docId);
            doc.setStatus(1);
            doc.setChunkCount(chunks.size());
            doc.setErrorMsg("");
            knowledgeDocMapper.updateById(doc);
            log.info("文档解析流水线完成: docId={}, 块数={}, 耗时={}ms", docId, chunks.size(),
                    System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("文档解析流水线失败: docId={}, 原因={}", docId, e.getMessage(), e);
            KnowledgeDoc doc = new KnowledgeDoc();
            doc.setId(docId);
            doc.setStatus(2);
            String msg = e.getMessage() == null ? "未知错误" : e.getMessage();
            doc.setErrorMsg(msg.length() > ERROR_MSG_MAX ? msg.substring(0, ERROR_MSG_MAX) : msg);
            knowledgeDocMapper.updateById(doc);
        }
    }

    /** 兜底查询用（供统计/调试） */
    public boolean existsDoc(Long docId) {
        return knowledgeDocMapper.selectCount(
                new LambdaQueryWrapper<KnowledgeDoc>().eq(KnowledgeDoc::getId, docId)) > 0;
    }
}
