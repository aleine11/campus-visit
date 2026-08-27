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
 * AI 问答消息实体类
 * 对应数据库表：chat_message
 * 状态字典 D6：role 字段值为 user / assistant
 *
 * 设计说明：
 * - visitor_id 冗余存储：方便 A7 问答日志统计时不必 JOIN chat_session
 * - refer_doc_id / refer_chunk：命中知识库时记录引用来源，前端展示"参考来源"
 * - tokens：本次回答消耗 token 数（来自百炼返回），用于统计成本
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("chat_message")
public class ChatMessage {

    /** 消息ID（主键自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联会话ID */
    private Long sessionId;

    /** 访客用户ID（冗余，便于统计） */
    private Long visitorId;

    /** 消息角色：user=访客提问，assistant=AI 回答 */
    private String role;

    /** 消息内容（user=问题原文，assistant=回答全文） */
    private String content;

    /** 引用文档ID（命中知识库时关联 knowledge_doc.id，未命中为 NULL） */
    private Long referDocId;

    /** 引用片段原文（截取 Milvus top-1 片段，前端展示"参考来源"） */
    private String referChunk;

    /** 本次回答消耗 token 数（来自百炼返回，统计用） */
    private Integer tokens;

    /** 创建时间（即问答时间） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
