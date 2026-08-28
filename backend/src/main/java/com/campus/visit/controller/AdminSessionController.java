package com.campus.visit.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.visit.annotation.RequiresRole;
import com.campus.visit.common.Result;
import com.campus.visit.dto.session.SessionQueryDTO;
import com.campus.visit.dto.session.SessionSaveDTO;
import com.campus.visit.service.SessionService;
import com.campus.visit.vo.session.SessionListVO;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 场次后台管理接口（全部需要管理员登录）
 *
 * 对标 architecture.md 模块 12（共 6 个接口）：
 *   GET    /api/admin/session/page            分页（日期/状态过滤）      🔒
 *   POST   /api/admin/session                 新增（禁过去日期）         🔒
 *   PUT    /api/admin/session/{id}            编辑（缩容保护）           🔒
 *   POST   /api/admin/session/{id}/online     上架                       🔒
 *   POST   /api/admin/session/{id}/offline    下架                       🔒
 *   DELETE /api/admin/session/{id}            删除（有预约禁删）         🔒
 */
@RestController
@RequestMapping("/admin/session")
@RequiresRole("admin")
public class AdminSessionController {

    @Resource
    private SessionService sessionService;

    /**
     * 12.1 场次分页（含下架和过期场次，按日期/状态过滤）
     */
    @GetMapping("/page")
    public Result<Page<SessionListVO>> page(SessionQueryDTO query) {
        return Result.success(sessionService.pageForAdmin(query));
    }

    /**
     * 12.2 新增场次（过去日期被 @FutureOrPresent 注解拦截 → 40001）
     */
    @PostMapping
    public Result<Long> save(@Valid @RequestBody SessionSaveDTO dto) {
        return Result.success(sessionService.save(dto));
    }

    /**
     * 12.3 编辑场次（缩容保护：maxPeople < usedPeople → 40022）
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody SessionSaveDTO dto) {
        sessionService.update(id, dto);
        return Result.success();
    }

    /**
     * 12.4 上架场次（下架 → 开放）
     */
    @PostMapping("/{id}/online")
    public Result<Void> online(@PathVariable Long id) {
        sessionService.online(id);
        return Result.success();
    }

    /**
     * 12.5 下架场次（开放 → 下架）
     */
    @PostMapping("/{id}/offline")
    public Result<Void> offline(@PathVariable Long id) {
        sessionService.offline(id);
        return Result.success();
    }

    /**
     * 12.6 删除场次（usedPeople > 0 → 40022 禁删）
     */
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        sessionService.remove(id);
        return Result.success();
    }
}
