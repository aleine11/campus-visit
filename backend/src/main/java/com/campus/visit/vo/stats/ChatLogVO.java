package com.campus.visit.vo.stats;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 问答日志列表条目 VO（对标 architecture.md 10.2）
 *
 * 一条记录 = 一次有效问答（以 assistant 消息为主表，回答成功才计入日志）。
 * question 不是日志表自带的字段，而是"同会话中该条回答之前的最新一条 user 消息"，
 * 由 Mapper 层三表关联 SQL 现场拼出来——多轮会话里问题与回答一一对应。
 */
@Data
public class ChatLogVO {

    /** 消息 ID（assistant 消息主键，日志排序/翻页基准） */
    private Long id;

    /** 访客姓名（join visitor_user.real_name） */
    private String visitorName;

    /** 访客问题（同会话上一条 user 消息内容） */
    private String question;

    /** AI 回答全文 */
    private String answer;

    /** 引用文档名（join knowledge_doc.file_name；未命中知识库或文档已删除时为 null） */
    private String referDocName;

    /** 回答时间（即该条 assistant 消息的创建时间） */
    private LocalDateTime createTime;
}
