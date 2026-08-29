package com.campus.visit.controller;

import com.campus.visit.annotation.RequiresLogin;
import com.campus.visit.common.Result;
import com.campus.visit.dto.visitor.VisitorProfileDTO;
import com.campus.visit.service.AdminVisitorService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 访客个人信息维护接口（对标 architecture.md 6.4）
 *
 *   PUT /api/visitor/profile   修改个人信息（姓名/手机号）🔒 @RequiresLogin
 *
 * 修改对象 = 当前登录人本人（UserContext 取），不收 id 参数 → 天然防越权
 */
@RestController
@RequestMapping("/visitor")
@RequiresLogin
public class VisitorProfileController {

    @Resource
    private AdminVisitorService adminVisitorService;

    /**
     * 6.4 访客修改个人信息
     * username 不可改（登录身份锚点）；密码走 /auth/password
     */
    @PutMapping("/profile")
    public Result<Void> updateProfile(@Valid @RequestBody VisitorProfileDTO dto) {
        adminVisitorService.updateProfile(dto);
        return Result.success();
    }
}
