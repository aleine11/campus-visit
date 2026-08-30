package com.campus.visit.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.campus.visit.annotation.RequiresRole;
import com.campus.visit.common.Result;
import com.campus.visit.service.StatsService;
import com.campus.visit.vo.stats.ChatLogVO;
import com.campus.visit.vo.stats.DashboardVO;
import com.campus.visit.vo.stats.HotKeywordsVO;
import jakarta.annotation.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 统计与问答日志接口（对标 architecture.md 模块 10，共 3 个端点，全部管理员）
 *
 *   GET /api/admin/stats/dashboard      后台首页看板        🔒 @RequiresRole("admin")
 *   GET /api/admin/stats/chat-log/page  问答日志分页        🔒
 *   GET /api/admin/stats/hot-keywords   高频问题统计        🔒
 *
 * 全部只读接口（SELECT 聚合），无任何写操作——管理员的"数据驾驶舱"。
 */
@RestController
@RequestMapping("/admin/stats")
@RequiresRole("admin")
public class StatsController {

    @Resource
    private StatsService statsService;

    /**
     * 10.1 后台首页看板
     * 4 个数字卡片 + 近 7 天预约趋势 + 最近 5 条待审核快捷入口
     */
    @GetMapping("/dashboard")
    public Result<DashboardVO> dashboard() {
        return Result.success(statsService.dashboard());
    }

    /**
     * 10.2 问答日志分页
     * 四个筛选条件全部可选：访客 / 问题关键词 / 时间范围；按回答时间倒序
     */
    @GetMapping("/chat-log/page")
    public Result<IPage<ChatLogVO>> chatLogPage(
            @RequestParam(required = false) Long visitorId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(
                statsService.pageChatLogs(visitorId, keyword, startDate, endDate, current, size));
    }

    /**
     * 10.3 高频问题统计
     * days 统计近 N 天（默认 30，取值 1~365，越界 40001）
     */
    @GetMapping("/hot-keywords")
    public Result<HotKeywordsVO> hotKeywords(@RequestParam(defaultValue = "30") Integer days) {
        return Result.success(statsService.hotKeywords(days));
    }
}
