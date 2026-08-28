package com.campus.visit.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.visit.common.Result;
import com.campus.visit.service.SessionService;
import com.campus.visit.vo.session.SessionDetailVO;
import com.campus.visit.vo.session.SessionListVO;
import jakarta.annotation.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 参观场次前台接口（全部公开，无需登录）
 *
 * 对标 architecture.md 模块 3（共 3 个接口）：
 *   GET /api/session/available   可预约场次分页   ❌ 公开
 *   GET /api/session/latest      最新可预约场次   ❌ 公开
 *   GET /api/session/{id}        场次详情         ❌ 公开
 *
 * @DateTimeFormat(iso = DATE)：把 "2026-09-01" 格式的 Query 参数转成 LocalDate
 */
@RestController
@RequestMapping("/session")
public class SessionController {

    @Resource
    private SessionService sessionService;

    /**
     * 3.1 可预约场次分页
     * 默认查今天起 30 天内；自动过滤下架和过期场次
     */
    @GetMapping("/available")
    public Result<Page<SessionListVO>> available(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(sessionService.listAvailable(startDate, endDate, current, size));
    }

    /**
     * 3.2 最新可预约场次（访客首页，默认 3 条）
     */
    @GetMapping("/latest")
    public Result<List<SessionListVO>> latest(
            @RequestParam(defaultValue = "3") Integer count) {
        return Result.success(sessionService.latest(count));
    }

    /**
     * 3.3 场次详情
     */
    @GetMapping("/{id}")
    public Result<SessionDetailVO> detail(@PathVariable Long id) {
        return Result.success(sessionService.detail(id));
    }
}
