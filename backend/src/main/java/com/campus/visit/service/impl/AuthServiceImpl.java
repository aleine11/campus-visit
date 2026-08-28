package com.campus.visit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.visit.common.BusinessException;
import com.campus.visit.common.ResultCode;
import com.campus.visit.dto.auth.ChangePasswordDTO;
import com.campus.visit.dto.auth.LoginDTO;
import com.campus.visit.dto.auth.RegisterDTO;
import com.campus.visit.entity.AdminUser;
import com.campus.visit.entity.VisitorUser;
import com.campus.visit.mapper.AdminUserMapper;
import com.campus.visit.mapper.VisitorUserMapper;
import com.campus.visit.service.AuthService;
import com.campus.visit.utils.BcryptUtil;
import com.campus.visit.utils.JwtUtil;
import com.campus.visit.utils.UserContext;
import com.campus.visit.utils.UserContext.LoginUser;
import com.campus.visit.vo.auth.LoginVO;
import com.campus.visit.vo.auth.ProfileVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 用户认证 Service 实现
 *
 * 业务逻辑严格对标 architecture.md 1.1 ~ 1.4：
 *   注册：先查重（访客表+管理员表都要查）→ BCrypt 加密 → insert
 *   登录：访客表 → 管理员表 → 都没匹配报 40010
 *   改密：按 UserContext.role 定位表 → BCrypt 验旧密码 → 加密新密码 update
 *   个人信息：按角色查表组装 ProfileVO
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    @Resource
    private VisitorUserMapper visitorUserMapper;

    @Resource
    private AdminUserMapper adminUserMapper;

    @Resource
    private BcryptUtil bcryptUtil;

    @Resource
    private JwtUtil jwtUtil;

    /** 角色常量：JWT payload 和数据库判断都用这两个字符串 */
    private static final String ROLE_VISITOR = "visitor";
    private static final String ROLE_ADMIN = "admin";

    /**
     * 访客注册
     *
     * 为什么访客表和管理员表都要查重？
     *   登录逻辑是"先查访客表再查管理员表"，
     *   如果允许访客注册一个和管理员相同的用户名，
     *   登录时访客表先命中，管理员账号就永远登不上了（被"遮蔽"）
     */
    @Override
    public void register(RegisterDTO dto) {
        // 1. 查访客表是否已有该用户名（LambdaQueryWrapper = 类型安全的 where 条件构造器）
        Long visitorCount = visitorUserMapper.selectCount(
                new LambdaQueryWrapper<VisitorUser>()
                        .eq(VisitorUser::getUsername, dto.getUsername()));
        if (visitorCount > 0) {
            throw new BusinessException(ResultCode.USERNAME_EXISTS);  // 40011
        }

        // 2. 查管理员表（防止遮蔽管理员账号）
        Long adminCount = adminUserMapper.selectCount(
                new LambdaQueryWrapper<AdminUser>()
                        .eq(AdminUser::getUsername, dto.getUsername()));
        if (adminCount > 0) {
            throw new BusinessException(ResultCode.USERNAME_EXISTS);  // 40011
        }

        // 3. 组装实体：密码必须 BCrypt 加密后入库（数据库永远不存明文密码）
        VisitorUser user = new VisitorUser();
        user.setUsername(dto.getUsername());
        user.setPassword(bcryptUtil.encode(dto.getPassword()));
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        user.setStatus(0);  // 0 = 正常（见状态字典 D1）
        user.setRegisterTime(LocalDateTime.now());

        // 4. 插入数据库（createTime/updateTime 由 MyBatis-Plus 自动填充）
        visitorUserMapper.insert(user);
        log.info("访客注册成功: username={}, id={}", user.getUsername(), user.getId());
    }

    /**
     * 统一登录（双表策略）
     *
     * 执行流程（对标架构文档 1.2 业务逻辑）：
     *   1. 查 visitor_user：
     *      - 用户名不存在 → 继续查管理员表
     *      - 存在但密码错 → 直接 40010（用户名在访客表里，没必要再查管理员表）
     *      - 密码对 + 状态冻结 → 40012
     *      - 密码对 + 状态正常 → 签发 visitor token
     *   2. 查 admin_user：
     *      - 不存在或密码错 → 40010（两表都试过了）
     *      - 匹配 → 签发 admin token（带 isSuper）
     */
    @Override
    public LoginVO login(LoginDTO dto) {
        // ========== 第一步：查访客表 ==========
        VisitorUser visitor = visitorUserMapper.selectOne(
                new LambdaQueryWrapper<VisitorUser>()
                        .eq(VisitorUser::getUsername, dto.getUsername()));

        if (visitor != null) {
            // 先校验密码（BCrypt 比对：明文 vs 数据库散列）
            if (!bcryptUtil.matches(dto.getPassword(), visitor.getPassword())) {
                throw new BusinessException(ResultCode.USERNAME_OR_PASSWORD_ERROR);  // 40010
            }
            // 密码对了再查状态：1 = 冻结（见状态字典 D1）
            if (visitor.getStatus() != null && visitor.getStatus() == 1) {
                throw new BusinessException(ResultCode.ACCOUNT_FROZEN);  // 40012
            }
            // 全部通过 → 签发访客 token（isSuper 恒为 false）
            String token = jwtUtil.generateToken(visitor.getId(), ROLE_VISITOR, false, visitor.getUsername());
            log.info("访客登录成功: {}", visitor.getUsername());
            return new LoginVO(token, ROLE_VISITOR, visitor.getId(), visitor.getRealName(), false);
        }

        // ========== 第二步：访客表没这个人 → 查管理员表 ==========
        AdminUser admin = adminUserMapper.selectOne(
                new LambdaQueryWrapper<AdminUser>()
                        .eq(AdminUser::getUsername, dto.getUsername()));

        // 两表都找不到，或者管理员密码不匹配 → 统一 40010
        // 安全细节：用户名不存在和密码错误返回同一个提示，防止攻击者探测"哪些用户名存在"
        if (admin == null || !bcryptUtil.matches(dto.getPassword(), admin.getPassword())) {
            throw new BusinessException(ResultCode.USERNAME_OR_PASSWORD_ERROR);  // 40010
        }

        // 管理员登录成功：is_super=1 表示超管
        boolean isSuper = admin.getIsSuper() != null && admin.getIsSuper() == 1;
        String token = jwtUtil.generateToken(admin.getId(), ROLE_ADMIN, isSuper, admin.getUsername());
        log.info("管理员登录成功: {}, isSuper={}", admin.getUsername(), isSuper);
        return new LoginVO(token, ROLE_ADMIN, admin.getId(), admin.getRealName(), isSuper);
    }

    /**
     * 修改密码（访客与管理员共用）
     *
     * "共用"的实现技巧：当前登录人的 role 存在 JWT 里 → 拦截器解析后放进 UserContext
     * 这里从 UserContext 拿 role 决定操作哪张表，一个方法服务两种用户
     */
    @Override
    public void changePassword(ChangePasswordDTO dto) {
        LoginUser loginUser = UserContext.get();  // 拦截器已保证非 null

        if (ROLE_VISITOR.equals(loginUser.getRole())) {
            // ===== 访客改密 =====
            VisitorUser user = visitorUserMapper.selectById(loginUser.getUserId());
            if (user == null) {
                throw new BusinessException(ResultCode.NOT_FOUND);
            }
            // BCrypt 校验旧密码
            if (!bcryptUtil.matches(dto.getOldPassword(), user.getPassword())) {
                throw new BusinessException(ResultCode.OLD_PASSWORD_ERROR);  // 40013
            }
            // 新密码不能与原密码相同（跨字段比较只能放业务层）
            if (dto.getNewPassword().equals(dto.getOldPassword())) {
                throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "新密码不能与原密码相同");
            }
            // 只更新密码字段（updateById 默认只更新非 null 字段）
            user.setPassword(bcryptUtil.encode(dto.getNewPassword()));
            visitorUserMapper.updateById(user);
            log.info("访客修改密码成功: {}", user.getUsername());
        } else {
            // ===== 管理员改密 =====
            AdminUser admin = adminUserMapper.selectById(loginUser.getUserId());
            if (admin == null) {
                throw new BusinessException(ResultCode.NOT_FOUND);
            }
            if (!bcryptUtil.matches(dto.getOldPassword(), admin.getPassword())) {
                throw new BusinessException(ResultCode.OLD_PASSWORD_ERROR);  // 40013
            }
            if (dto.getNewPassword().equals(dto.getOldPassword())) {
                throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "新密码不能与原密码相同");
            }
            admin.setPassword(bcryptUtil.encode(dto.getNewPassword()));
            adminUserMapper.updateById(admin);
            log.info("管理员修改密码成功: {}", admin.getUsername());
        }
        // 安全提示：改密后原 token 依然有效（JWT 无状态），
        // 毕设场景可接受；生产级系统需要配合 Redis token 黑名单，这里不做过度设计
    }

    /**
     * 当前登录人信息
     *
     * 数据来源分两层：
     *   userId / role / username / isSuper → JWT 载荷（UserContext 里现成的）
     *   realName / phone → 需要查数据库拿最新值（用户可能改过资料）
     */
    @Override
    public ProfileVO profile() {
        LoginUser loginUser = UserContext.get();

        if (ROLE_VISITOR.equals(loginUser.getRole())) {
            // 访客：查表拿 realName + phone，isSuper 固定 false
            VisitorUser visitor = visitorUserMapper.selectById(loginUser.getUserId());
            if (visitor == null) {
                throw new BusinessException(ResultCode.NOT_FOUND);
            }
            return ProfileVO.builder()
                    .userId(visitor.getId())
                    .role(ROLE_VISITOR)
                    .username(visitor.getUsername())
                    .realName(visitor.getRealName())
                    .phone(visitor.getPhone())      // 访客有手机号
                    .isSuper(false)                 // 访客恒为 false
                    .build();
        }

        // 管理员：查表拿 realName，phone 为 null，isSuper 从数据库取
        AdminUser admin = adminUserMapper.selectById(loginUser.getUserId());
        if (admin == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return ProfileVO.builder()
                .userId(admin.getId())
                .role(ROLE_ADMIN)
                .username(admin.getUsername())
                .realName(admin.getRealName())
                .phone(null)  // 管理员无手机号字段，按文档返回 null
                .isSuper(admin.getIsSuper() != null && admin.getIsSuper() == 1)
                .build();
    }
}
