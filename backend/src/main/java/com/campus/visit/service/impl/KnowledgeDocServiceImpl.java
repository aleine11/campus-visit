package com.campus.visit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.visit.common.BusinessException;
import com.campus.visit.common.ResultCode;
import com.campus.visit.entity.AdminUser;
import com.campus.visit.entity.KnowledgeDoc;
import com.campus.visit.mapper.AdminUserMapper;
import com.campus.visit.mapper.KnowledgeDocMapper;
import com.campus.visit.rag.DocParseUtil;
import com.campus.visit.rag.DocumentPipeline;
import com.campus.visit.rag.MilvusService;
import com.campus.visit.service.KnowledgeDocService;
import com.campus.visit.utils.UserContext;
import com.campus.visit.vo.knowledge.KnowledgeListVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * RAG 知识库文档管理 Service 实现（对标 architecture.md 模块 8）
 *
 * 一致性顺序（database.md 6.4）：跨 MySQL + Milvus 无法强一致，
 * 删除/重解析时采用"先 Milvus 后 MySQL"——Milvus 挂了 MySQL 原数据还在，
 * 修好 Milvus 后 reparse 即可恢复，绝不出现"MySQL 删了但向量残留"的脏数据。
 */
@Slf4j
@Service
public class KnowledgeDocServiceImpl implements KnowledgeDocService {

    /** 允许上传的类型（设计文档约定：pdf/txt/docx） */
    private static final Set<String> ALLOWED_TYPES = Set.of("pdf", "txt", "docx");

    private static final int STATUS_PARSING = 0;
    private static final int STATUS_DONE = 1;
    private static final int STATUS_FAILED = 2;

    @Resource
    private KnowledgeDocMapper knowledgeDocMapper;
    @Resource
    private AdminUserMapper adminUserMapper;
    @Resource
    private MilvusService milvusService;
    @Resource
    private DocumentPipeline documentPipeline;

    /**
     * 8.1 文档分页（fileName 模糊 + fileType + status）
     * 上传人姓名批量回填（selectBatchIds 防 N+1，模块 4 同款优化）
     */
    @Override
    public Page<KnowledgeListVO> page(String fileName, String fileType, Integer status,
                                      Integer current, Integer size) {
        int page = (current == null || current < 1) ? 1 : current;
        int rows = (size == null || size < 1) ? 10 : size;

        Page<KnowledgeDoc> result = knowledgeDocMapper.selectPage(new Page<>(page, rows),
                new LambdaQueryWrapper<KnowledgeDoc>()
                        .like(fileName != null && !fileName.isBlank(), KnowledgeDoc::getFileName, fileName)
                        .eq(fileType != null && !fileType.isBlank(), KnowledgeDoc::getFileType, fileType)
                        .eq(status != null, KnowledgeDoc::getStatus, status)
                        .orderByDesc(KnowledgeDoc::getCreateTime));

        // 批量取上传人姓名
        Set<Long> adminIds = result.getRecords().stream()
                .map(KnowledgeDoc::getUploadAdminId).collect(Collectors.toSet());
        Map<Long, String> adminNames = adminIds.isEmpty() ? Map.of()
                : adminUserMapper.selectBatchIds(adminIds).stream()
                        .collect(Collectors.toMap(AdminUser::getId,
                                a -> a.getRealName() == null ? a.getUsername() : a.getRealName()));

        Page<KnowledgeListVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream()
                .map(d -> toVo(d, adminNames.get(d.getUploadAdminId()))).toList());
        return voPage;
    }

    /**
     * 8.2 上传文档
     * 同步部分：类型校验 → 保存物理文件 → 插 MySQL(status=0) → 丢给异步流水线
     * 返回文档 ID 后前端轮询 8.1 列表的 status 字段
     */
    @Override
    public Long upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "上传文件不能为空");
        }
        String originalName = file.getOriginalFilename() == null ? "unknown" : file.getOriginalFilename();
        String ext = DocParseUtil.extension(originalName);
        if (!ALLOWED_TYPES.contains(ext)) {
            throw new BusinessException(ResultCode.DOC_TYPE_NOT_SUPPORT);       // 40030
        }

        // 物理文件：./uploads/knowledge/{yyyy}/{uuid}.{ext}
        String year = String.valueOf(LocalDate.now().getYear());
        String stored = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path dir = Paths.get("uploads", "knowledge", year);
        Path target = dir.resolve(stored);
        try {
            Files.createDirectories(dir);
            file.transferTo(target.toAbsolutePath());
        } catch (Exception e) {
            throw new BusinessException(ResultCode.SERVER_ERROR, "文件保存失败: " + e.getMessage());
        }

        // 元数据入库（status=0 解析中）
        KnowledgeDoc doc = new KnowledgeDoc();
        doc.setFileName(originalName);
        doc.setFileType(ext);
        doc.setFilePath(target.toString());
        doc.setFileSize(file.getSize());
        doc.setChunkCount(0);
        doc.setStatus(STATUS_PARSING);
        doc.setUploadAdminId(UserContext.get().getUserId());
        doc.setErrorMsg("");
        knowledgeDocMapper.insert(doc);

        // 丢给异步流水线（跨 Bean 调用保证 @Async 代理生效）
        documentPipeline.processAsync(doc.getId(), target.toAbsolutePath().toString(), originalName);
        log.info("文档已上传待解析: id={}, name={}, size={}", doc.getId(), originalName, file.getSize());
        return doc.getId();
    }

    /**
     * 8.3 删除文档：先删 Milvus 向量（失败 40050 不动 MySQL）→ 删 MySQL → 删物理文件
     */
    @Override
    public void delete(Long id) {
        KnowledgeDoc doc = getById(id);                                          // 40401
        milvusService.deleteByDocId(id);                                         // 失败抛 40050
        knowledgeDocMapper.deleteById(id);
        try {
            Files.deleteIfExists(Paths.get(doc.getFilePath()).toAbsolutePath());
        } catch (Exception e) {
            log.warn("物理文件删除失败（不影响业务）: {}", e.getMessage());
        }
        log.info("知识库文档已删除: id={}, name={}", id, doc.getFileName());
    }

    /**
     * 8.4 重新解析：清旧向量 → 重置状态 → 重走异步流水线（物理文件必须还在）
     */
    @Override
    public void reparse(Long id) {
        KnowledgeDoc doc = getById(id);                                          // 40401
        if (!Files.exists(Paths.get(doc.getFilePath()).toAbsolutePath())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "物理文件已丢失，无法重新解析");
        }
        milvusService.deleteByDocId(id);                                         // 先清旧向量
        KnowledgeDoc reset = new KnowledgeDoc();
        reset.setId(id);
        reset.setStatus(STATUS_PARSING);
        reset.setChunkCount(0);
        reset.setErrorMsg("");
        knowledgeDocMapper.updateById(reset);
        documentPipeline.processAsync(id, Paths.get(doc.getFilePath()).toAbsolutePath().toString(),
                doc.getFileName());
    }

    /* ==================== 私有方法 ==================== */

    private KnowledgeDoc getById(Long id) {
        KnowledgeDoc doc = knowledgeDocMapper.selectById(id);
        if (doc == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);                   // 40401
        }
        return doc;
    }

    private KnowledgeListVO toVo(KnowledgeDoc d, String adminName) {
        return KnowledgeListVO.builder()
                .id(d.getId())
                .fileName(d.getFileName())
                .fileType(d.getFileType())
                .fileSize(d.getFileSize())
                .chunkCount(d.getChunkCount())
                .status(d.getStatus())
                .statusText(switch (d.getStatus()) {
                    case STATUS_DONE -> "已完成";
                    case STATUS_FAILED -> "失败";
                    default -> "解析中";
                })
                .uploadAdminName(adminName)
                .createTime(d.getCreateTime())
                .errorMsg(d.getErrorMsg() == null || d.getErrorMsg().isBlank() ? null : d.getErrorMsg())
                .build();
    }
}
