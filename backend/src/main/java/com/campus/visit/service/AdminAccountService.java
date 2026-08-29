package com.campus.visit.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.visit.dto.admin.AdminSaveDTO;
import com.campus.visit.vo.admin.AdminListVO;

/**
 * 管理员账号管理 Service（对标 architecture.md 模块 7，全部仅超管）
 *
 * page           7.1 管理员分页
 * create         7.2 新增管理员（普通管理员身份；超管只能数据库手动设）
 * resetPassword  7.3 重置任意管理员密码
 */
public interface AdminAccountService {

    /** 管理员分页（keyword 模糊匹配账号/姓名） */
    Page<AdminListVO> page(String keyword, Integer current, Integer size);

    /** 新增管理员，返回新管理员 ID（账号唯一：双表查重防登录遮蔽） */
    Long create(AdminSaveDTO dto);

    /** 重置管理员密码（超管忘记密码互救机制） */
    void resetPassword(Long id, String newPassword);
}
