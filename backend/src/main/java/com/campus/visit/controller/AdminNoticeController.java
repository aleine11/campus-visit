package com.campus.visit.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.visit.annotation.RequiresRole;
import com.campus.visit.common.Result;
import com.campus.visit.dto.notice.NoticeQueryDTO;
import com.campus.visit.dto.notice.NoticeSaveDTO;
import com.campus.visit.service.NoticeService;
import com.campus.visit.vo.notice.NoticeListVO;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公告后台管理接口（全部需要管理员登录）
 *
 * 对标 architecture.md 补充模块 11（共 6 个接口）：
 *   GET    /api/admin/notice/page             分页（keyword 模糊 + status 过滤）🔒
 *   POST   /api/admin/notice                  新增（草稿或直接发布）             🔒
 *   PUT    /api/admin/notice/{id}             编辑                              🔒
 *   POST   /api/admin/notice/{id}/publish     发布（草稿→已发布）               🔒
 *   POST   /api/admin/notice/{id}/offline     下架（已发布→草稿）               🔒
 *   DELETE /api/admin/notice/{id}             删除（逻辑删除）                  🔒
 *
 * 类上挂 @RequiresRole("admin")：本类所有接口都要求管理员角色
 * （拦截器会校验 JWT 里的 role=admin，普通访客访问直接 403）
 */
@RestController
@RequestMapping("/admin/notice")
@RequiresRole("admin")
public class AdminNoticeController {

    @Resource
    private NoticeService noticeService;

    /**
     * 11.1 公告分页（含草稿，支持标题模糊搜索和状态过滤）
     * GET 参数自动绑定到 NoticeQueryDTO 字段（keyword/status/current/size）
     */
    @GetMapping("/page")
    public Result<Page<NoticeListVO>> page(NoticeQueryDTO query) {
        return Result.success(noticeService.pageForAdmin(query));
    }

    /**
     * 11.2 新增公告（status=0 存草稿 / status=1 直接发布）
     * 响应 data = 新公告的 ID
     */
    @PostMapping
    public Result<Long> save(@Valid @RequestBody NoticeSaveDTO dto) {
        return Result.success(noticeService.save(dto));
    }

    /**
     * 11.3 编辑公告（只改标题和正文，不动发布状态）
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody NoticeSaveDTO dto) {
        noticeService.update(id, dto);
        return Result.success();
    }

    /**
     * 11.4 发布公告（草稿 → 已发布，记录发布人和发布时间）
     */
    @PostMapping("/{id}/publish")
    public Result<Void> publish(@PathVariable Long id) {
        noticeService.publish(id);
        return Result.success();
    }

    /**
     * 11.5 下架公告（已发布 → 草稿）
     */
    @PostMapping("/{id}/offline")
    public Result<Void> offline(@PathVariable Long id) {
        noticeService.offline(id);
        return Result.success();
    }

    /**
     * 11.6 删除公告（逻辑删除，前台立即不可见）
     */
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        noticeService.remove(id);
        return Result.success();
    }
}
