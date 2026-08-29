package com.campus.visit.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.visit.annotation.RequiresRole;
import com.campus.visit.common.Result;
import com.campus.visit.service.KnowledgeDocService;
import com.campus.visit.vo.knowledge.KnowledgeListVO;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * RAG 知识库文档管理接口（对标 architecture.md 模块 8，全部管理员）
 *
 *   GET    /api/admin/knowledge/page            文档分页      🔒 @RequiresRole("admin")
 *   POST   /api/admin/knowledge/upload          上传文档      🔒 multipart/form-data
 *   DELETE /api/admin/knowledge/{id}            删除文档      🔒
 *   POST   /api/admin/knowledge/{id}/reparse    重新解析      🔒
 *
 * 上传为异步解析：本接口立即返回文档 ID，前端轮询 page 接口的 status 字段
 */
@RestController
@RequestMapping("/admin/knowledge")
@RequiresRole("admin")
public class AdminKnowledgeDocController {

    @Resource
    private KnowledgeDocService knowledgeDocService;

    /**
     * 8.1 文档分页（fileName 模糊 / fileType / status 过滤）
     */
    @GetMapping("/page")
    public Result<Page<KnowledgeListVO>> page(
            @RequestParam(required = false) String fileName,
            @RequestParam(required = false) String fileType,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(knowledgeDocService.page(fileName, fileType, status, current, size));
    }

    /**
     * 8.2 上传文档（pdf/txt/docx，≤50MB 由全局 multipart 配置限制）
     */
    @PostMapping("/upload")
    public Result<Long> upload(@RequestParam("file") MultipartFile file) {
        return Result.success(knowledgeDocService.upload(file));
    }

    /**
     * 8.3 删除文档（Milvus + MySQL + 物理文件三清理）
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        knowledgeDocService.delete(id);
        return Result.success();
    }

    /**
     * 8.4 重新解析（失败文档修复入口）
     */
    @PostMapping("/{id}/reparse")
    public Result<Void> reparse(@PathVariable Long id) {
        knowledgeDocService.reparse(id);
        return Result.success();
    }
}
