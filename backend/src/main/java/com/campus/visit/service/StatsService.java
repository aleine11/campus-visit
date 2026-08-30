package com.campus.visit.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.visit.vo.stats.ChatLogVO;
import com.campus.visit.vo.stats.DashboardVO;
import com.campus.visit.vo.stats.HotKeywordsVO;

import java.time.LocalDateTime;

/**
 * 统计与问答日志 Service（对标 architecture.md 模块 10）
 *
 * 三个能力，全部只读（统计不修改任何业务数据）：
 *   dashboard()     —— 后台首页看板（4 数字 + 7 天趋势 + 5 条待审）
 *   pageChatLogs()  —— 问答日志分页（管理员回溯"谁问了什么、AI 答了什么"）
 *   hotKeywords()   —— 高频问题统计（优化知识库/FAQ 的依据）
 */
public interface StatsService {

    /**
     * 10.1 后台首页看板
     */
    DashboardVO dashboard();

    /**
     * 10.2 问答日志分页
     *
     * @param visitorId 按访客筛选，null=全部
     * @param keyword   问题关键词模糊，null/空=不过滤
     * @param startDate 回答时间起始（含），null=不限
     * @param endDate   回答时间截止（含），null=不限
     * @param current   页码（从 1 开始）
     * @param size      每页条数
     */
    IPage<ChatLogVO> pageChatLogs(Long visitorId, String keyword,
                                  LocalDateTime startDate, LocalDateTime endDate,
                                  Integer current, Integer size);

    /**
     * 10.3 高频问题统计（近 N 天用户提问的字频 Top10 + Top100）
     *
     * @param days 统计天数，1~365
     */
    HotKeywordsVO hotKeywords(Integer days);
}
