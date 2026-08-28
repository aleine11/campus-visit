package com.campus.visit.service;

import com.campus.visit.dto.auth.ChangePasswordDTO;
import com.campus.visit.dto.auth.LoginDTO;
import com.campus.visit.dto.auth.RegisterDTO;
import com.campus.visit.vo.auth.LoginVO;
import com.campus.visit.vo.auth.ProfileVO;

/**
 * 用户认证 Service 接口
 *
 * 对标 architecture.md 模块 1 的 4 个接口：
 *   register        访客注册
 *   login           统一登录（访客 + 管理员双表）
 *   changePassword  修改密码（访客 + 管理员共用）
 *   profile         当前登录人信息
 *
 * 为什么先定义接口再写实现类？（面向接口编程）
 *   1. 职责清晰：接口 = "能做什么"，实现类 = "怎么做"
 *   2. 便于替换：以后换实现（比如加验证码登录），Controller 一行不用改
 *   3. 便于测试：可以注入 Mock 实现做单元测试
 */
public interface AuthService {

    /** 访客注册：校验用户名唯一 → BCrypt 加密密码 → 入库 */
    void register(RegisterDTO dto);

    /** 统一登录：先查访客表再查管理员表，成功签发 JWT */
    LoginVO login(LoginDTO dto);

    /** 修改密码：根据当前登录角色改对应表，BCrypt 校验旧密码 */
    void changePassword(ChangePasswordDTO dto);

    /** 当前登录人信息：根据角色查对应表组装 VO */
    ProfileVO profile();
}
