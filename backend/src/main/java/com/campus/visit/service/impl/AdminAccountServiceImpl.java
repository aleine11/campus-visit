package com.campus.visit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.visit.common.BusinessException;
import com.campus.visit.common.ResultCode;
import com.campus.visit.dto.admin.AdminSaveDTO;
import com.campus.visit.entity.AdminUser;
import com.campus.visit.mapper.AdminUserMapper;
import com.campus.visit.mapper.VisitorUserMapper;
import com.campus.visit.service.AdminAccountService;
import com.campus.visit.utils.BcryptUtil;
import com.campus.visit.utils.UserContext;
import com.campus.visit.vo.admin.AdminListVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 管理员账号管理 Service 实现（对标 architecture.md 模块 7）
 *
 * ⭐ 双表查重（防登录遮蔽）：
 * 登录策略是"先查访客表再查管理员表"，如果新建管理员的账号和某访客重名，
 * 该管理员将永远登录不进去（每次都命中访客表然后密码不对）。
 * 所以新增管理员时必须同时查 visitor_user 和 admin_user 两张表。
 *
 * ⭐ 新增的管理员永远是普通管理员（is_super=0）：
 * 超管身份只能数据库手动改，防止超管页面"造超管"权限失控
 */
@Slf4j
@Service
public class AdminAccountServiceImpl implements AdminAccountService {

    /** 普通管理员 */
    private static final int NOT_SUPER = 0;

    @Resource
    private AdminUserMapper adminUserMapper;
    @Resource
    private VisitorUserMapper visitorUserMapper;
    /** BCrypt 工具（Spring Bean，实例方法注入使用） */
    @Resource
    private BcryptUtil bcryptUtil;

    /**
     * 7.1 管理员分页（keyword 模糊匹配账号/姓名）
     */
    @Override
    public Page<AdminListVO> page(String keyword, Integer current, Integer size) {
        int page = (current == null || current < 1) ? 1 : current;
        int rows = (size == null || size < 1) ? 10 : size;

        boolean hasKeyword = keyword != null && !keyword.isBlank();
        Page<AdminUser> result = adminUserMapper.selectPage(new Page<>(page, rows),
                new LambdaQueryWrapper<AdminUser>()
                        // 两列模糊必须括号包裹（防 or 泄漏，模块 6 知识点 11.1 同款）
                        .and(hasKeyword, w -> w.like(AdminUser::getUsername, keyword)
                                .or().like(AdminUser::getRealName, keyword))
                        .orderByAsc(AdminUser::getId));

        Page<AdminListVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toVo).toList());
        return voPage;
    }

    /**
     * 7.2 新增管理员
     *
     * 流程：双表查重（40011）→ BCrypt 加密 → 入库（is_super 固定 0）
     */
    @Override
    public Long create(AdminSaveDTO dto) {
        String username = dto.getUsername().trim();

        // 双表查重：管理表自身 + 访客表（防遮蔽，原理见类注释）
        Long adminDup = adminUserMapper.selectCount(
                new LambdaQueryWrapper<AdminUser>().eq(AdminUser::getUsername, username));
        if (adminDup > 0) {
            throw new BusinessException(ResultCode.USERNAME_EXISTS); // 40011
        }
        Long visitorDup = visitorUserMapper.selectCount(
                new LambdaQueryWrapper<com.campus.visit.entity.VisitorUser>()
                        .eq(com.campus.visit.entity.VisitorUser::getUsername, username));
        if (visitorDup > 0) {
            throw new BusinessException(ResultCode.USERNAME_EXISTS); // 40011
        }

        AdminUser admin = new AdminUser();
        admin.setUsername(username);
        admin.setPassword(bcryptUtil.encode(dto.getPassword())); // 明文→BCrypt 散列
        admin.setRealName(dto.getRealName().trim());
        admin.setIsSuper(NOT_SUPER); // 固定普通管理员
        adminUserMapper.insert(admin);

        log.info("新增管理员: id={}, username={}, 操作人={}",
                admin.getId(), username, com.campus.visit.utils.UserContext.get().getUsername());
        return admin.getId();
    }

    /**
     * 7.3 重置管理员密码
     * 存在性校验（40401）→ 新密码 BCrypt 加密 → 更新
     */
    @Override
    public void resetPassword(Long id, String newPassword) {
        AdminUser admin = adminUserMapper.selectById(id);
        if (admin == null) {
            throw new BusinessException(ResultCode.NOT_FOUND); // 40401
        }
        admin.setPassword(bcryptUtil.encode(newPassword));
        adminUserMapper.updateById(admin);
        log.info("重置管理员密码: id={}, username={}", id, admin.getUsername());
    }

    /** Entity → VO（password 绝不映射） */
    private AdminListVO toVo(AdminUser a) {
        return AdminListVO.builder()
                .id(a.getId())
                .username(a.getUsername())
                .realName(a.getRealName())
                .isSuper(a.getIsSuper() != null && a.getIsSuper() == 1)
                .createTime(a.getCreateTime())
                .build();
    }
}
