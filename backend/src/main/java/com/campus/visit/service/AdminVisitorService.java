package com.campus.visit.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.visit.dto.visitor.VisitorProfileDTO;
import com.campus.visit.vo.visitor.VisitorListVO;

/**
 * 访客用户管理 Service（对标 architecture.md 模块 6）
 *
 * pageForAdmin  6.1 访客分页（管理员，keyword 模糊 + 状态过滤）
 * freeze        6.2 冻结访客（status→1，冻结后无法登录）
 * unfreeze      6.3 解冻访客（status→0）
 * updateProfile 6.4 访客修改个人信息（本人操作）
 */
public interface AdminVisitorService {

    /** 访客分页查询（keyword 同时模糊匹配 用户名/姓名/手机号） */
    Page<VisitorListVO> pageForAdmin(String keyword, Integer status, Integer current, Integer size);

    /** 冻结访客（重复冻结 → 40022 状态机拦截） */
    void freeze(Long id);

    /** 解冻访客（重复解冻 → 40022 状态机拦截） */
    void unfreeze(Long id);

    /** 访客修改个人信息（当前登录人自己的资料） */
    void updateProfile(VisitorProfileDTO dto);
}
