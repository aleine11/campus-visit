package com.campus.visit.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.visit.vo.knowledge.KnowledgeListVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * RAG 知识库文档管理 Service（对标 architecture.md 模块 8，4 接口全部管理员）
 *
 * page      8.1 文档分页（fileName 模糊 + fileType + status 过滤）
 * upload    8.2 上传文档（保存文件 → MySQL status=0 → 异步解析流水线）
 * delete    8.3 删除文档（先 Milvus 清向量 → 删 MySQL → 删物理文件）
 * reparse   8.4 重新解析（清旧向量 → 重置状态 → 再走上传的异步链路）
 */
public interface KnowledgeDocService {

    /** 文档分页 */
    Page<KnowledgeListVO> page(String fileName, String fileType, Integer status, Integer current, Integer size);

    /** 上传文档，返回文档 ID（同步返回，解析异步进行） */
    Long upload(MultipartFile file);

    /** 删除文档（Milvus 失败 → 抛 40050 且不删 MySQL） */
    void delete(Long id);

    /** 重新解析 */
    void reparse(Long id);
}
