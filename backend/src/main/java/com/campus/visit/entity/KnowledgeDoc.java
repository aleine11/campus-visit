package com.campus.visit.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 知识库文档实体类
 * 对应数据库表：knowledge_doc
 * 状态字典 D5：0=解析中，1=已完成，2=失败
 *
 * 与 Milvus 同步规则：
 * - 解析成功后，文本分块 + 向量写入 Milvus 的 campus_knowledge 集合
 * - 删除文档时，同步删除 Milvus 中对应向量
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("knowledge_doc")
public class KnowledgeDoc {

    /** 文档ID（主键自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 原始文件名（如 "校园简介.pdf"） */
    private String fileName;

    /** 文件类型：pdf / txt / docx */
    private String fileType;

    /** 文件存储路径（服务器绝对路径或相对路径） */
    private String filePath;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 文本分块数（RAG 解析后写入） */
    private Integer chunkCount;

    /** 解析状态：0=解析中，1=已完成，2=失败 */
    private Integer status;

    /** 上传人管理员ID */
    private Long uploadAdminId;

    /** 失败原因（status=2 时写入） */
    private String errorMsg;

    /** 创建时间（MyBatis-Plus 自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间（MyBatis-Plus 自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
