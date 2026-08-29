package com.campus.visit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.visit.common.BusinessException;
import com.campus.visit.common.ResultCode;
import com.campus.visit.dto.visitor.VisitorProfileDTO;
import com.campus.visit.entity.VisitorUser;
import com.campus.visit.mapper.VisitorUserMapper;
import com.campus.visit.service.AdminVisitorService;
import com.campus.visit.utils.UserContext;
import com.campus.visit.vo.visitor.VisitorListVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 访客用户管理 Service 实现（对标 architecture.md 模块 6）
 *
 * 状态字典 D1：0=正常 1=冻结
 * 冻结联动：AuthServiceImpl 登录时校验 status=1 → 抛 40012 拒绝登录
 *（本模块只负责改状态，登录拦截逻辑在模块 1 已实现——这里实测验证联动）
 */
@Slf4j
@Service
public class AdminVisitorServiceImpl implements AdminVisitorService {

    private static final int STATUS_NORMAL = 0;
    private static final int STATUS_FROZEN = 1;

    @Resource
    private VisitorUserMapper visitorUserMapper;

    /**
     * 6.1 访客分页（管理员）
     *
     * keyword 一词三搜：用户名 / 姓名 / 手机号 任一命中即返回（or 拼接）
     * 写法要点：like 条件包在 .and(w -> ...) 里，保证生成
     *   WHERE (username LIKE ? OR real_name LIKE ? OR phone LIKE ?) AND status = ?
     * 而不是 or 破坏外层的 status 过滤条件（经典坑！）
     */
    @Override
    public Page<VisitorListVO> pageForAdmin(String keyword, Integer status, Integer current, Integer size) {
        int page = (current == null || current < 1) ? 1 : current;
        int rows = (size == null || size < 1) ? 10 : size;

        boolean hasKeyword = keyword != null && !keyword.isBlank();
        Page<VisitorUser> result = visitorUserMapper.selectPage(new Page<>(page, rows),
                new LambdaQueryWrapper<VisitorUser>()
                        // 关键词多列模糊：三列任一命中（括号包裹防止 or 泄漏）
                        .and(hasKeyword, w -> w.like(VisitorUser::getUsername, keyword)
                                .or().like(VisitorUser::getRealName, keyword)
                                .or().like(VisitorUser::getPhone, keyword))
                        .eq(status != null, VisitorUser::getStatus, status)
                        .orderByDesc(VisitorUser::getCreateTime));

        Page<VisitorListVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toVo).toList());
        return voPage;
    }

    /**
     * 6.2 冻结访客
     * 状态机：仅 正常(0)→冻结(1)；已冻结再冻结 → 40022（幂等保护，避免误操作刷审计日志）
     */
    @Override
    public void freeze(Long id) {
        updateStatus(id, STATUS_FROZEN, "冻结");
    }

    /**
     * 6.3 解冻访客
     * 状态机：仅 冻结(1)→正常(0)；已正常再解冻 → 40022
     */
    @Override
    public void unfreeze(Long id) {
        updateStatus(id, STATUS_NORMAL, "解冻");
    }

    /**
     * 6.4 访客修改个人信息（本人）
     * 从 UserContext 取当前登录人 ID —— 身份永远由服务端 JWT 自证
     */
    @Override
    public void updateProfile(VisitorProfileDTO dto) {
        Long visitorId = UserContext.get().getUserId();
        VisitorUser user = visitorUserMapper.selectById(visitorId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        user.setRealName(dto.getRealName().trim());
        user.setPhone(dto.getPhone().trim());
        visitorUserMapper.updateById(user);
        log.info("访客修改个人信息: id={}", visitorId);
    }

    /* ==================== 私有工具方法 ==================== */

    /** 冻结/解冻共用逻辑：存在性校验 → 状态机校验 → 更新 */
    private void updateStatus(Long id, int targetStatus, String action) {
        VisitorUser user = visitorUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);                  // 40401
        }
        if (user.getStatus() == targetStatus) {
            // 已经是目标状态 → 重复操作拦截（40022 状态类）
            throw new BusinessException(ResultCode.RESERVATION_STATUS_INVALID);
        }
        user.setStatus(targetStatus);
        visitorUserMapper.updateById(user);
        log.info("访客{}: id={}, username={}", action, id, user.getUsername());
    }

    /** Entity → VO（password 绝不映射） */
    private VisitorListVO toVo(VisitorUser u) {
        return VisitorListVO.builder()
                .id(u.getId())
                .username(u.getUsername())
                .realName(u.getRealName())
                .phone(u.getPhone())
                .status(u.getStatus())
                .statusText(u.getStatus() == STATUS_NORMAL ? "正常" : "冻结")
                .registerTime(u.getCreateTime())
                .build();
    }
}
