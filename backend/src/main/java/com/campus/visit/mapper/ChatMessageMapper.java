package com.campus.visit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.visit.entity.ChatMessage;
import com.campus.visit.vo.stats.ChatLogVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

/**
 * AI 问答消息 Mapper
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    /**
     * 问答日志分页（对标 architecture.md 10.2，自定义三表关联 SQL）
     *
     * 思路：一条日志 = 一次 AI 回答，所以主表是 role='assistant' 的消息。
     * 三表关联：
     *   ① JOIN visitor_user        —— 补访客姓名（每条消息必有 visitor_id，内连接）
     *   ② LEFT JOIN chat_message q —— 取"同会话中该回答之前的最新一条 user 消息"当问题
     *      （相关子查询 MAX(q2.id) 保证多轮会话里问答一一对应，不串轮）
     *   ③ LEFT JOIN knowledge_doc  —— 补引用文档名；
     *      故意不校验 d.deleted，已删文档的历史日志仍显示文档名（日志是历史快照）
     *
     * 分页：第一个参数传 IPage，MyBatis-Plus 分页插件自动拼 LIMIT 和 COUNT，SQL 不用写。
     * 动态筛选：<if> 标签按需拼接，全为 null 时查全部。
     * 转义说明：<script> 内 SQL 的 < > 必须写实体 &lt; &gt;，否则被当 XML 标签解析报错。
     *
     * @param page      分页对象（current/size 由 Controller 层传入）
     * @param visitorId 按访客筛选，null=全部访客
     * @param keyword   问题关键词模糊匹配，null/空串=不过滤
     * @param startDate 回答时间起始（含），null=不限
     * @param endDate   回答时间截止（含），null=不限
     */
    @Select("""
            <script>
            SELECT
                m.id           AS id,
                v.real_name    AS visitor_name,
                q.content      AS question,
                m.content      AS answer,
                d.file_name    AS refer_doc_name,
                m.create_time  AS create_time
            FROM chat_message m
            JOIN visitor_user v ON m.visitor_id = v.id
            LEFT JOIN chat_message q ON q.id = (
                SELECT MAX(q2.id) FROM chat_message q2
                WHERE q2.session_id = m.session_id
                  AND q2.role = 'user'
                  AND q2.id &lt; m.id
            )
            LEFT JOIN knowledge_doc d ON m.refer_doc_id = d.id
            WHERE m.role = 'assistant'
            <if test="visitorId != null">
                AND m.visitor_id = #{visitorId}
            </if>
            <if test="keyword != null and keyword != ''">
                AND q.content LIKE CONCAT('%', #{keyword}, '%')
            </if>
            <if test="startDate != null">
                AND m.create_time &gt;= #{startDate}
            </if>
            <if test="endDate != null">
                AND m.create_time &lt;= #{endDate}
            </if>
            ORDER BY m.id DESC
            </script>
            """)
    IPage<ChatLogVO> pageChatLogs(IPage<ChatLogVO> page,
                                  @Param("visitorId") Long visitorId,
                                  @Param("keyword") String keyword,
                                  @Param("startDate") LocalDateTime startDate,
                                  @Param("endDate") LocalDateTime endDate);
}
