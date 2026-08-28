package com.campus.visit.controller;

import com.campus.visit.common.Result;
import com.campus.visit.service.NoticeService;
import com.campus.visit.vo.notice.NoticeDetailVO;
import com.campus.visit.vo.notice.NoticeListVO;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 校园公告前台接口（全部公开，无需登录）
 *
 * 对标 architecture.md 模块 2（共 3 个接口）：
 *   GET /api/notice/list      公告分页列表    ❌ 公开
 *   GET /api/notice/latest    最新 N 条公告   ❌ 公开
 *   GET /api/notice/{id}      公告详情        ❌ 公开
 *
 * GET 请求用 @RequestParam 接收 Query 参数（?current=1&size=10）
 * 不加 @RequiresLogin 注解 → 拦截器直接放行
 */
@RestController
@RequestMapping("/notice")
public class NoticeController {

    @Resource
    private NoticeService noticeService;

    /**
     * 2.1 公告分页列表（仅已发布，按发布时间倒序）
     *
     * defaultValue：参数不传时自动用默认值，比 Service 里判 null 更简洁
     */
    @GetMapping("/list")
    public Result<List<NoticeListVO>> list(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(noticeService.listPublished(current, size));
    }

    /**
     * 2.2 最新公告（访客首页展示，默认 3 条，最多 10 条）
     */
    @GetMapping("/latest")
    public Result<List<NoticeListVO>> latest(
            @RequestParam(defaultValue = "3") Integer count) {
        return Result.success(noticeService.latest(count));
    }

    /**
     * 2.3 公告详情（含上一篇/下一篇 ID）
     *
     * @PathVariable：提取 URL 路径中的 {id}
     * 访问 /api/notice/5 → id=5
     */
    @GetMapping("/{id}")
    public Result<NoticeDetailVO> detail(@PathVariable Long id) {
        return Result.success(noticeService.detail(id));
    }
}
