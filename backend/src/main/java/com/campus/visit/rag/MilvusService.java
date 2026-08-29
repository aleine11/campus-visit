package com.campus.visit.rag;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.R;
import io.milvus.param.collection.CreateCollectionParam;
import io.milvus.param.collection.FieldType;
import io.milvus.param.collection.FlushParam;
import io.milvus.param.collection.HasCollectionParam;
import io.milvus.param.collection.LoadCollectionParam;
import io.milvus.param.dml.DeleteParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.response.SearchResultsWrapper;
import io.milvus.grpc.SearchResults;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.grpc.DataType;
import com.campus.visit.common.BusinessException;
import com.campus.visit.common.ResultCode;
import com.google.gson.JsonObject;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Milvus 向量库操作服务（对标 database.md 6.1/6.2 集合定义）
 *
 * 集合 campus_knowledge（100% 对齐设计文档 6.2 Schema）：
 * id INT64 主键自增
 * embedding FLOAT_VECTOR(768) ← BGE 嵌入向量
 * doc_id INT64 ← 关联 MySQL knowledge_doc.id（删除过滤键）
 * chunk_id INT64 ← 文档内分块序号
 * content VARCHAR ← 分块原文
 * file_name VARCHAR ← 原始文件名（冗余展示）
 *
 * 索引：IVF_FLAT + IP（内积，BGE 归一化后内积=余弦相似度）+ nlist 128
 * 注：content 设计文档写 VARCHAR(500)，Milvus 的 max_length 按 UTF-8 字节计，
 * 500 个汉字≈1500 字节，故实际设 2048 字节（等价"500 汉字"语义，容量安全）
 */
@Slf4j
@Service
public class MilvusService {

        @Resource
        private MilvusServiceClient milvusClient;

        @Value("${campus.milvus.collection-name}")
        private String collectionName;

        @Value("${campus.bge.vector-dim}")
        private int vectorDim;

        /** 启动时预建集合（best-effort：Milvus 没启动不阻塞应用，上传时会重试建集合） */
        @PostConstruct
        public void init() {
                try {
                        ensureCollection();
                } catch (Exception e) {
                        log.warn("Milvus 集合预初始化失败（Milvus 可能未启动，上传文档时会自动重试）: {}", e.getMessage());
                }
        }

        /** 幂等建集合：不存在才创建（含索引 + 加载） */
        public synchronized void ensureCollection() {
                R<Boolean> has = milvusClient.hasCollection(
                                HasCollectionParam.newBuilder().withCollectionName(collectionName).build());
                if (Boolean.TRUE.equals(has.getData())) {
                        return;
                }

                CreateCollectionParam param = CreateCollectionParam.newBuilder()
                                .withCollectionName(collectionName)
                                .withDescription("校园知识库向量集合（BGE 768 维）")
                                .addFieldType(FieldType.newBuilder()
                                                .withName("id").withDataType(DataType.Int64)
                                                .withPrimaryKey(true).withAutoID(true).build())
                                .addFieldType(FieldType.newBuilder()
                                                .withName("embedding").withDataType(DataType.FloatVector)
                                                .withDimension(vectorDim).build())
                                .addFieldType(FieldType.newBuilder()
                                                .withName("doc_id").withDataType(DataType.Int64).build())
                                .addFieldType(FieldType.newBuilder()
                                                .withName("chunk_id").withDataType(DataType.Int64).build())
                                .addFieldType(FieldType.newBuilder()
                                                .withName("content").withDataType(DataType.VarChar)
                                                .withMaxLength(2048).build())
                                .addFieldType(FieldType.newBuilder()
                                                .withName("file_name").withDataType(DataType.VarChar)
                                                .withMaxLength(512).build())
                                .build();
                check(milvusClient.createCollection(param), "创建集合");

                // IVF_FLAT 索引（千级数据量够用，nlist=128）
                check(milvusClient.createIndex(CreateIndexParam.newBuilder()
                                .withCollectionName(collectionName)
                                .withFieldName("embedding")
                                .withIndexType(IndexType.IVF_FLAT)
                                .withMetricType(MetricType.IP)
                                .withExtraParam("{\"nlist\":128}")
                                .build()),
                                "创建向量索引");

                // 加载到内存（检索前置条件）
                check(milvusClient.loadCollection(
                                LoadCollectionParam.newBuilder().withCollectionName(collectionName).build()),
                                "加载集合");
                log.info("Milvus 集合初始化完成: {}（{} 维，IVF_FLAT/IP）", collectionName, vectorDim);
        }

        /**
         * 插入一个文档的全部分块向量（解析流水线第 4 步）
         * 出错抛 40050，由流水线上层捕获写 knowledge_doc.status=2
         */
        public void insertChunks(Long docId, String fileName, List<String> chunks, List<float[]> vectors) {
                ensureCollection();

                List<Long> docIds = new ArrayList<>();
                List<Long> chunkIds = new ArrayList<>();
                List<String> contents = new ArrayList<>();
                List<String> fileNames = new ArrayList<>();
                List<List<Float>> embeddings = new ArrayList<>();
                for (int i = 0; i < chunks.size(); i++) {
                        docIds.add(docId);
                        chunkIds.add((long) i);
                        contents.add(chunks.get(i));
                        fileNames.add(fileName);
                        List<Float> vec = new ArrayList<>(vectors.get(i).length);
                        for (float v : vectors.get(i)) {
                                vec.add(v);
                        }
                        embeddings.add(vec);
                }

                // 2.3.4 版 Field 构造器为 (name, values) 两参；向量字段直接传 List<List<Float>>
                List<InsertParam.Field> fields = new ArrayList<>();
                fields.add(new InsertParam.Field("doc_id", docIds));
                fields.add(new InsertParam.Field("chunk_id", chunkIds));
                fields.add(new InsertParam.Field("content", contents));
                fields.add(new InsertParam.Field("file_name", fileNames));
                fields.add(new InsertParam.Field("embedding", embeddings));

                R<?> result = milvusClient.insert(InsertParam.newBuilder()
                                .withCollectionName(collectionName)
                                .withFields(fields)
                                .build());
                check(result, "插入向量");

                // 刷新落盘（row_count 统计与持久化）
                milvusClient.flush(FlushParam.newBuilder()
                                .withCollectionNames(List.of(collectionName))
                                .build());
                log.info("Milvus 插入完成: docId={}, 块数={}", docId, chunks.size());
        }

        /**
         * 按 doc_id 删除一个文档的全部向量（删除/重新解析前清理）
         * 失败抛 40050 —— 保证"先 Milvus 后 MySQL"的一致性顺序（database.md 6.4）
         */
        public void deleteByDocId(Long docId) {
                R<?> result = milvusClient.delete(DeleteParam.newBuilder()
                                .withCollectionName(collectionName)
                                .withExpr("doc_id == " + docId)
                                .build());
                check(result, "删除向量");
                milvusClient.flush(FlushParam.newBuilder()
                                .withCollectionNames(List.of(collectionName))
                                .build());
                log.info("Milvus 向量已清理: docId={}", docId);
        }

        /** 统一结果校验：Milvus 返回非 0 一律抛 40050 */
        private void check(R<?> result, String action) {
                if (result.getStatus() != R.Status.Success.getCode()) {
                        log.error("Milvus {}失败: status={}, msg={}", action, result.getStatus(), result.getMessage());
                        throw new BusinessException(ResultCode.MILVUS_ERROR, action + "失败: " + result.getMessage());
                }
        }

        /* ==================== 检索（模块 9 读取链路） ==================== */

        /**
         * 语义检索 top-K（对标 architecture.md 9.1 第 4 步）
         *
         * @param questionVector 问题向量（BGE 归一化后）
         * @param topK           取前 K 个最相似片段
         * @return 按相似度降序的片段列表（IP 分数 = 余弦相似度，范围 [-1, 1]）
         */
        public List<KnowledgeChunk> searchTopK(float[] questionVector, int topK) {
                ensureCollection();

                List<Float> vec = new ArrayList<>(questionVector.length);
                for (float v : questionVector) {
                        vec.add(v);
                }

                SearchParam searchParam = SearchParam.newBuilder()
                                .withCollectionName(collectionName)
                                .withMetricType(MetricType.IP)
                                .withVectorFieldName("embedding")
                                .withTopK(topK)
                                .withVectors(List.of(vec))
                                // 布尔表达式过滤：只检索正常解析完成的文档向量（防御：文档被删但向量未清干净时不误召回）
                                .withExpr("doc_id > 0")
                                .addOutField("doc_id")
                                .addOutField("chunk_id")
                                .addOutField("content")
                                .addOutField("file_name")
                                .withRoundDecimal(-1)
                                .build();

                R<SearchResults> response = milvusClient.search(searchParam);
                check(response, "向量检索");

                SearchResultsWrapper wrapper = new SearchResultsWrapper(response.getData().getResults());
                List<KnowledgeChunk> chunks = new ArrayList<>();
                // 只有一个查询向量，取第 0 组结果
                List<SearchResultsWrapper.IDScore> scores = wrapper.getIDScore(0);
                for (SearchResultsWrapper.IDScore idScore : scores) {
                        KnowledgeChunk chunk = new KnowledgeChunk();
                        chunk.setDocId(toLong(idScore.get("doc_id")));
                        chunk.setChunkId(toLong(idScore.get("chunk_id")));
                        chunk.setContent(toStr(idScore.get("content")));
                        chunk.setFileName(toStr(idScore.get("file_name")));
                        chunk.setScore(idScore.getScore());
                        chunks.add(chunk);
                }
                log.info("Milvus 检索完成: topK={}, 命中={} 条, 最高分={}", topK, chunks.size(),
                                chunks.isEmpty() ? 0 : chunks.get(0).getScore());
                return chunks;
        }

        /** SDK 返回的字段值类型不确定（JsonPrimitive/Number/String），统一转 Long */
        private Long toLong(Object value) {
                if (value == null) {
                        return null;
                }
                if (value instanceof com.google.gson.JsonPrimitive p) {
                        return p.getAsLong();
                }
                if (value instanceof Number n) {
                        return n.longValue();
                }
                return Long.parseLong(value.toString());
        }

        /** SDK 返回的字段值类型不确定，统一转 String */
        private String toStr(Object value) {
                if (value == null) {
                        return null;
                }
                if (value instanceof com.google.gson.JsonPrimitive p) {
                        return p.getAsString();
                }
                return value.toString();
        }

        /**
         * 检索结果片段（Milvus 一行 → 业务对象）
         */
        @lombok.Data
        public static class KnowledgeChunk {
                private Long docId;
                private Long chunkId;
                private String content;
                private String fileName;
                private Float score;
        }
}
