package com.campus.visit.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * AI 提问请求 DTO（对标 architecture.md 9.1 ChatAskDTO）
 */
@Data
public class ChatAskDTO {

    /** 会话 ID：null 时自动新建会话（首问场景） */
    private Long sessionId;

    /** 访客问题（1~500 字） */
    @NotBlank(message = "问题内容不能为空")
    @Size(min = 1, max = 500, message = "问题长度须在 1~500 字之间")
    private String question;
}
