package com.campus.visit.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.visit.annotation.RequiresRole;
import com.campus.visit.common.Result;
import com.campus.visit.service.AdminVisitorService;
import com.campus.visit.vo.visitor.VisitorListVO;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 访客用户管理后台接口（对标 architecture.md 6.1~6.3，全部管理员）
 *
 *   GET  /api/admin/visitor/page            访客分页      🔒 @RequiresRole("admin")
 *   POST /api/admin/visitor/{id}/freeze     冻结访客      🔒
 *   POST /api/admin/visitor/{id}/unfreeze   解冻访客      🔒
 *
 * 冻结效果：该访客再登录 → 40012（模块 1 登录逻辑联动）
 */
@RestController
@RequestMapping("/admin/visitor")
@RequiresRole("admin")
public class AdminVisitorController {

    @Resource
    private AdminVisitorService adminVisitorService;

    /**
     * 6.1 访客分页
     * keyword 一词三搜（用户名/姓名/手机号）；status 0正常/1冻结 过滤
     */
    @GetMapping("/page")
    public Result<Page<VisitorListVO>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(adminVisitorService.pageForAdmin(keyword, status, current, size));
    }

    /**
     * 6.2 冻结访客（重复冻结 → 40022）
     */
    @PostMapping("/{id}/freeze")
    public Result<Void> freeze(@PathVariable Long id) {
        adminVisitorService.freeze(id);
        return Result.success();
    }

    /**
     * 6.3 解冻访客（重复解冻 → 40022）
     */
    @PostMapping("/{id}/unfreeze")
    public Result<Void> unfreeze(@PathVariable Long id) {
        adminVisitorService.unfreeze(id);
        return Result.success();
    }
}
