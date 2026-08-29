package com.campus.visit.controller;

import com.campus.visit.common.Result;
import com.campus.visit.dto.chat.ChatAskDTO;
import com.campus.visit.service.ChatService;
import com.campus.visit.vo.chat.ChatAskVO;
import com.campus.visit.vo.chat.ChatMessageVO;
import com.campus.visit.vo.chat.ChatSessionVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.visit.annotation.RequiresLogin;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 智能咨询接口（对标 architecture.md 模块 9，5 端点全部访客登录态）
 */
@RestController
@RequestMapping("/chat")
@RequiresLogin
public class ChatController {

    @Resource
    private ChatService chatService;

    /** 9.1 提问（sessionId 为空自动建会话） */
    @PostMapping("/ask")
    public Result<ChatAskVO> ask(@Valid @RequestBody ChatAskDTO dto) {
        return Result.success(chatService.ask(dto));
    }

    /** 9.2 我的会话分页 */
    @GetMapping("/session/my")
    public Result<IPage<ChatSessionVO>> mySessions(@RequestParam(defaultValue = "1") long pageNum,
                                                   @RequestParam(defaultValue = "10") long pageSize) {
        return Result.success(chatService.pageMySessions(pageNum, pageSize));
    }

    /** 9.3 会话消息分页 */
    @GetMapping("/session/{id}/messages")
    public Result<IPage<ChatMessageVO>> messages(@PathVariable Long id,
                                                 @RequestParam(defaultValue = "1") long pageNum,
                                                 @RequestParam(defaultValue = "20") long pageSize) {
        return Result.success(chatService.pageMessages(id, pageNum, pageSize));
    }

    /** 9.4 新建空会话 */
    @PostMapping("/session/new")
    public Result<Long> newSession() {
        return Result.success(chatService.createSession());
    }

    /** 9.5 清空（删除）会话 */
    @DeleteMapping("/session/my/{id}")
    public Result<Void> clearSession(@PathVariable Long id) {
        chatService.clearSession(id);
        return Result.success(null);
    }
}
