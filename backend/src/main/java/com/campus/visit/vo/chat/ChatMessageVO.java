package com.campus.visit.vo.chat;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话消息条目 VO（对标 architecture.md 9.3 ChatMessageVO）
 */
@Data
@Builder
public class ChatMessageVO {

    private Long id;

    /** 消息角色：user / assistant */
    private String role;

    /** 消息内容 */
    private String content;

    /** 引用文档名（仅 assistant 且命中知识库时有值） */
    private String referDocName;

    private LocalDateTime createTime;
}
