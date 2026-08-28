package com.campus.visit.controller;

import com.campus.visit.annotation.RequiresLogin;
import com.campus.visit.common.Result;
import com.campus.visit.dto.auth.ChangePasswordDTO;
import com.campus.visit.dto.auth.LoginDTO;
import com.campus.visit.dto.auth.RegisterDTO;
import com.campus.visit.service.AuthService;
import com.campus.visit.vo.auth.LoginVO;
import com.campus.visit.vo.auth.ProfileVO;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户认证接口层
 *
 * 对标 architecture.md 模块 1（共 4 个接口）：
 *   POST /api/auth/register          访客注册        ❌ 公开
 *   POST /api/auth/login             统一登录        ❌ 公开
 *   POST /api/auth/change-password   修改密码        ✅ 需登录
 *   GET  /api/auth/profile           当前登录人信息   ✅ 需登录
 *
 * 路径说明：application.yml 配置了 context-path=/api，
 * 所以 @RequestMapping("/auth") 实际完整路径是 /api/auth/xxx
 *
 * 分层职责：Controller 只做"接参数 → 调 Service → 包 Result"三件事，
 * 不写任何业务逻辑（业务全在 Service），这叫"薄控制器"原则
 */
@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Resource
    private AuthService authService;

    /**
     * 1.1 访客注册
     *
     * @Valid 触发 RegisterDTO 上的校验注解（@NotBlank/@Pattern/@Size）
     * 校验失败抛 MethodArgumentNotValidException → 全局异常处理器转 40001
     */
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO dto) {
        authService.register(dto);
        return Result.success();
    }

    /**
     * 1.2 统一登录（访客 + 管理员）
     *
     * 登录成功返回 LoginVO（含 JWT token），前端存起来
     * 之后每次请求在 Authorization 头带 "Bearer {token}"
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(authService.login(dto));
    }

    /**
     * 1.3 修改密码
     *
     * @RequiresLogin：拦截器要求携带有效 JWT，并把登录人写入 UserContext
     * Service 里通过 UserContext.get().getRole() 区分访客/管理员
     */
    @RequiresLogin
    @PostMapping("/change-password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        authService.changePassword(dto);
        return Result.success();
    }

    /**
     * 1.4 当前登录人信息
     *
     * 前端页面初始化时调用，显示头像/姓名/角色等
     */
    @RequiresLogin
    @GetMapping("/profile")
    public Result<ProfileVO> profile() {
        return Result.success(authService.profile());
    }
}
