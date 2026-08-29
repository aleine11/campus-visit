package com.campus.visit.vo.chat;

import lombok.Builder;
import lombok.Data;

/**
 * AI 提问响应 VO（对标 architecture.md 9.1 ChatAskVO）
 */
@Data
@Builder
public class ChatAskVO {

    /** 会话 ID（新建会话时返回新 ID，前端要记住） */
    private Long sessionId;

    /** AI 回答全文 */
    private String answer;

    /** 引用文档名（未命中知识库时为 null） */
    private String referDocName;

    /** 引用片段原文（未命中知识库时为 null） */
    private String referChunk;
}
