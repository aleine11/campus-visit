package com.campus.visit.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.visit.common.BusinessException;
import com.campus.visit.common.ResultCode;
import com.campus.visit.dto.chat.ChatAskDTO;
import com.campus.visit.entity.ChatMessage;
import com.campus.visit.entity.ChatSession;
import com.campus.visit.mapper.ChatMessageMapper;
import com.campus.visit.mapper.ChatSessionMapper;
import com.campus.visit.rag.DashScopeClient;
import com.campus.visit.rag.EmbeddingService;
import com.campus.visit.rag.MilvusService;
import com.campus.visit.utils.UserContext;
import com.campus.visit.vo.chat.ChatAskVO;
import com.campus.visit.vo.chat.ChatMessageVO;
import com.campus.visit.vo.chat.ChatSessionVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 智能咨询业务（对标 architecture.md 模块 9 五接口 + RAG 完整工作链路）
 *
 * 读取链路（ask 方法，答辩主线）：
 * 问题 → BGE 向量化 → Milvus top3 检索 → 相似度阈值判断（防幻觉闸门）
 * → 命中：拼提示词调大模型 / 未命中：固定话术 → 消息落库 → 返回
 */
@Slf4j
@Service
public class ChatService {

    /** 相似度阈值：top1 低于此分视为"知识库没有"（宁说不知道，不编造） */
    private static final float SCORE_THRESHOLD = 0.5f;

    /** 检索条数（设计文档定稿 top-3） */
    private static final int TOP_K = 3;

    /** 系统提示词：把大模型"关进知识库笼子"的核心（防幻觉第一闸门） */
    private static final String SYSTEM_PROMPT = "你是哈尔滨剑桥学院的校园参观咨询助手。你必须只根据下面提供的知识库片段回答用户问题，"
            + "禁止使用任何知识库之外的信息，禁止编造。如果片段信息不足以回答，"
            + "直接回复：知识库未查询到相关内容，请咨询人工老师。";

    /** 未命中固定话术（设计文档 RAG 流程第 4 步原文） */
    private static final String FALLBACK_ANSWER = "知识库未查询到相关内容，请咨询人工老师。";

    @Resource
    private ChatSessionMapper chatSessionMapper;

    @Resource
    private ChatMessageMapper chatMessageMapper;

    @Resource
    private EmbeddingService embeddingService;

    @Resource
    private MilvusService milvusService;

    @Resource
    private DashScopeClient dashScopeClient;

    /* ==================== 9.1 提问（核心） ==================== */

    @Transactional(rollbackFor = Exception.class)
    public ChatAskVO ask(ChatAskDTO dto) {
        Long visitorId = UserContext.getUserId();

        // ① 会话处理：首次提问自动建会话，标题 = 问题前 20 字
        ChatSession session;
        if (dto.getSessionId() == null) {
            session = new ChatSession();
            session.setVisitorId(visitorId);
            session.setTitle(dto.getQuestion().length() > 20
                    ? dto.getQuestion().substring(0, 20)
                    : dto.getQuestion());
            chatSessionMapper.insert(session);
        } else {
            session = chatSessionMapper.selectById(dto.getSessionId());
            if (session == null) {
                throw new BusinessException(ResultCode.NOT_FOUND, "会话不存在");
            }
            // 越权校验：会话必须是当前访客的（40301 范式）
            if (!session.getVisitorId().equals(visitorId)) {
                throw new BusinessException(ResultCode.FORBIDDEN, "无权访问他人会话");
            }
        }

        // ② 用户消息先落库（即使 AI 调用失败，提问记录也保留）
        ChatMessage userMsg = new ChatMessage();
        userMsg.setSessionId(session.getId());
        userMsg.setVisitorId(visitorId);
        userMsg.setRole("user");
        userMsg.setContent(dto.getQuestion());
        chatMessageMapper.insert(userMsg);

        // ③ RAG 读取链路：向量化 → 检索 top3
        float[] questionVector = embeddingService.embed(dto.getQuestion());
        List<MilvusService.KnowledgeChunk> chunks = milvusService.searchTopK(questionVector, TOP_K);

        String answer;
        String referDocName = null;
        String referChunk = null;
        Integer tokens = null;
        Long referDocId = null;

        // ④ 防幻觉闸门：top1 相似度 < 0.5 → 不调大模型，直接固定话术
        if (chunks.isEmpty() || chunks.get(0).getScore() < SCORE_THRESHOLD) {
            log.info("知识库未命中: top1Score={}, 回固定话术",
                    chunks.isEmpty() ? "无结果" : chunks.get(0).getScore());
            answer = FALLBACK_ANSWER;
        } else {
            // ⑤ 命中：拼用户提示词（编号片段列表 + 问题）调大模型
            StringBuilder context = new StringBuilder();
            for (int i = 0; i < chunks.size(); i++) {
                MilvusService.KnowledgeChunk c = chunks.get(i);
                context.append("片段").append(i + 1).append("（来源: ").append(c.getFileName()).append("）：\n")
                        .append(c.getContent()).append("\n\n");
            }
            String userPrompt = "知识库片段：\n" + context + "用户问题：" + dto.getQuestion();

            DashScopeClient.LlmResult llm = dashScopeClient.chat(SYSTEM_PROMPT, userPrompt);
            answer = llm.answer();
            tokens = llm.totalTokens();
            // 引用取 top1（最相似片段）
            referDocId = chunks.get(0).getDocId();
            referDocName = chunks.get(0).getFileName();
            referChunk = chunks.get(0).getContent();
        }

        // ⑥ AI 回复落库（引用三元组存进消息，前端气泡点开可看出处）
        ChatMessage aiMsg = new ChatMessage();
        aiMsg.setSessionId(session.getId());
        aiMsg.setVisitorId(visitorId);
        aiMsg.setRole("assistant");
        aiMsg.setContent(answer);
        aiMsg.setReferDocId(referDocId);
        aiMsg.setReferChunk(referChunk);
        aiMsg.setTokens(tokens);
        chatMessageMapper.insert(aiMsg);

        return ChatAskVO.builder()
                .sessionId(session.getId())
                .answer(answer)
                .referDocName(referDocName)
                .referChunk(referChunk)
                .build();
    }

    /* ==================== 9.2 我的会话分页 ==================== */

    public IPage<ChatSessionVO> pageMySessions(long pageNum, long pageSize) {
        Long visitorId = UserContext.getUserId();

        Page<ChatSession> page = chatSessionMapper.selectPage(
                new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<ChatSession>()
                        .eq(ChatSession::getVisitorId, visitorId)
                        .orderByDesc(ChatSession::getUpdateTime));

        // 批量补 lastMessageTime（N+1 优化：一次查出本页所有会话的消息时间，内存取 max）
        List<Long> sessionIds = page.getRecords().stream().map(ChatSession::getId).toList();
        Map<Long, LocalDateTime> timeMap = new HashMap<>();
        if (!sessionIds.isEmpty()) {
            List<ChatMessage> msgs = chatMessageMapper.selectList(
                    new LambdaQueryWrapper<ChatMessage>()
                            .in(ChatMessage::getSessionId, sessionIds)
                            .select(ChatMessage::getSessionId, ChatMessage::getCreateTime));
            // 同会话多条消息保留最新时间
            msgs.forEach(m -> timeMap.merge(m.getSessionId(), m.getCreateTime(),
                    (a, b) -> a.isAfter(b) ? a : b));
        }
        // lambda 里要用的变量必须是 effectively final
        final Map<Long, LocalDateTime> lastTimeMap = timeMap;

        IPage<ChatSessionVO> result = page.convert(s -> {
            ChatSessionVO vo = new ChatSessionVO();
            vo.setId(s.getId());
            vo.setTitle(s.getTitle());
            vo.setCreateTime(s.getCreateTime());
            vo.setLastMessageTime(lastTimeMap.get(s.getId()));
            return vo;
        });
        // 兜底排序：lastMessageTime 降序、null 排最后（微信式最近优先）
        result.getRecords().sort(Comparator.comparing(
                ChatSessionVO::getLastMessageTime,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
    }

    /* ==================== 9.3 会话消息分页 ==================== */

    public IPage<ChatMessageVO> pageMessages(Long sessionId, long pageNum, long pageSize) {
        // 归属校验
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "会话不存在");
        }
        if (!session.getVisitorId().equals(UserContext.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权访问他人会话");
        }

        Page<ChatMessage> page = chatMessageMapper.selectPage(
                new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
                        .orderByAsc(ChatMessage::getCreateTime));

        return page.convert(m -> ChatMessageVO.builder()
                .id(m.getId())
                .role(m.getRole())
                .content(m.getContent())
                .createTime(m.getCreateTime())
                .build());
    }

    /* ==================== 9.4 新建空会话 ==================== */

    public Long createSession() {
        ChatSession session = new ChatSession();
        session.setVisitorId(UserContext.getUserId());
        chatSessionMapper.insert(session);
        return session.getId();
    }

    /* ==================== 9.5 清空会话（先删消息再删会话） ==================== */

    @Transactional(rollbackFor = Exception.class)
    public void clearSession(Long sessionId) {
        // 归属校验
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "会话不存在");
        }
        if (!session.getVisitorId().equals(UserContext.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权访问他人会话");
        }

        // 先删消息，再删会话（删子表再删主表，防孤儿消息）
        chatMessageMapper.delete(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, sessionId));
        chatSessionMapper.deleteById(sessionId);
        log.info("会话已清空: sessionId={}, visitor={}", sessionId, UserContext.get().getUsername());
    }
}
