package com.campus.visit.vo.chat;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话列表条目 VO（对标 architecture.md 9.2 ChatSessionVO）
 */
@Data
public class ChatSessionVO {

    private Long id;

    /** 会话标题（首问前 20 字；未提问过则为 null） */
    private String title;

    private LocalDateTime createTime;

    /** 最后一条消息时间（子查询 max(create_time)） */
    private LocalDateTime lastMessageTime;
}
