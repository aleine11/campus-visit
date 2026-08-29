package com.campus.visit.vo.knowledge;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库文档列表条目 VO（对标 architecture.md 8.1 KnowledgeListVO）
 */
@Data
@Builder
public class KnowledgeListVO {

    /** 文档 ID */
    private Long id;

    /** 原始文件名 */
    private String fileName;

    /** 类型：pdf/txt/docx */
    private String fileType;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 向量块数（解析完成后写入） */
    private Integer chunkCount;

    /** 解析状态：0=解析中 1=已完成 2=失败（状态字典 D5） */
    private Integer status;

    /** 状态中文：解析中 / 已完成 / 失败 */
    private String statusText;

    /** 上传人（管理员姓名） */
    private String uploadAdminName;

    /** 上传时间 */
    private LocalDateTime createTime;

    /** 失败原因（status=2 时有值） */
    private String errorMsg;
}
