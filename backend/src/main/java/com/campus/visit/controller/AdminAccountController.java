package com.campus.visit.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.visit.annotation.RequiresRole;
import com.campus.visit.common.Result;
import com.campus.visit.dto.admin.AdminSaveDTO;
import com.campus.visit.dto.admin.ResetPasswordDTO;
import com.campus.visit.service.AdminAccountService;
import com.campus.visit.vo.admin.AdminListVO;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员账号管理接口（对标 architecture.md 模块 7，全部仅超级管理员）
 *
 *   GET  /api/admin/admin/page                  管理员分页      🔒 @RequiresRole("admin") + superOnly
 *   POST /api/admin/admin                       新增管理员      🔒
 *   POST /api/admin/admin/{id}/reset-password   重置密码        🔒
 *
 * ⭐ 权限分层（二级门禁）：
 *   @RequiresRole(value = "admin", superOnly = true)
 *   拦截器两连问：① 是管理员吗？② 是超管吗？——任一不过 → 40301
 *   这是模块 0 预埋的 superOnly 属性首次启用
 */
@RestController
@RequestMapping("/admin/admin")
@RequiresRole(value = "admin", superOnly = true)
public class AdminAccountController {

    @Resource
    private AdminAccountService adminAccountService;

    /**
     * 7.1 管理员分页（keyword 模糊匹配账号/姓名）
     */
    @GetMapping("/page")
    public Result<Page<AdminListVO>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(adminAccountService.page(keyword, current, size));
    }

    /**
     * 7.2 新增管理员（返回新管理员 ID；账号与访客/管理员双表查重）
     */
    @PostMapping
    public Result<Long> create(@Valid @RequestBody AdminSaveDTO dto) {
        return Result.success(adminAccountService.create(dto));
    }

    /**
     * 7.3 重置管理员密码（管理员忘记密码的互救机制）
     */
    @PostMapping("/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordDTO dto) {
        adminAccountService.resetPassword(id, dto.getNewPassword());
        return Result.success();
    }
}
